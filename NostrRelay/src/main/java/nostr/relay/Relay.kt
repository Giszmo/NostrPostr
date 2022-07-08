package nostr.relay

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.JsonArray
import com.google.gson.JsonSyntaxException
import io.javalin.Javalin
import io.javalin.http.staticfiles.Location
import io.javalin.websocket.WsContext
import io.javalin.websocket.WsMessageContext
import nostr.postr.Client
import nostr.postr.Filter
import nostr.postr.Relay
import nostr.postr.events.Event
import nostr.postr.toHex
import nostr.relay.Events.createdAt
import nostr.relay.Events.hash
import nostr.relay.Events.kind
import nostr.relay.Events.pubKey
import nostr.relay.Events.raw
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.TransactionManager
import org.jetbrains.exposed.sql.transactions.transaction
import java.sql.Connection
import java.util.*

val gson: Gson = GsonBuilder().create()
val subscribers = mutableMapOf<WsContext, MutableMap<String, List<Filter>>>()
val featureList = mapOf(
    "id" to "ws://localhost:7070/",
    "name" to "NostrPostrRelay",
    "description" to "Relay running NostrPostr.",
    "pubkey" to "46fcbe3065eaf1ae7811465924e48923363ff3f526bd6f73d7c184b16bd8ce4d",
    "supported_nips" to listOf(1, 2, 9, 11, 12, 15, 16),
    "software" to "https://github.com/Giszmo/NostrPostr",
    "version" to "0"
)

fun main() {
    Database.connect("jdbc:sqlite:events.db", "org.sqlite.JDBC")
    TransactionManager.manager.defaultIsolationLevel =
        Connection.TRANSACTION_SERIALIZABLE
    // or Connection.TRANSACTION_READ_UNCOMMITTED
    transaction {
        addLogger(StdOutSqlLogger)
        SchemaUtils.createMissingTablesAndColumns(Events, Tags)
    }
    Javalin.create {
        it.addStaticFiles { staticFiles ->
            staticFiles.hostedPath = "/test"
            staticFiles.directory = "/public"
            staticFiles.location = Location.CLASSPATH
        }
    }.apply {
        get("/") {
            if (it.header("Accept") == "application/nostr+json") {
                it.json(featureList)
            } else {
                it.redirect("/test/")
            }
        }
        ws("/") { ws ->
            ws.onConnect { ctx ->
                subscribers[ctx] = subscribers[ctx] ?: mutableMapOf()
                ctx.send("""["NOTICE","Greetings stranger! This relay runs NostrPostr version 0.8. Please report bugs to https://github.com/Giszmo/NostrPostr"]""")
            }
            ws.onMessage { ctx ->
                val msg = ctx.message()
                try {
                    val jsonArray = gson.fromJson(msg, JsonArray::class.java)
                    when (val cmd = jsonArray[0].asString) {
                        "REQ" -> {
                            val channel = jsonArray[1].asString
                            val filters = jsonArray
                                .filterIndexed { index, _ -> index > 1 }
                                .mapIndexed { index, it ->
                                    try {
                                        Filter.fromJson(it.asJsonObject)
                                    } catch (e: Exception) {
                                        ctx.send("""["NOTICE","Something went wrong with filter $index. Ignoring request."]""")
                                        println("Something went wrong with filter $index. Ignoring request.\n${it}")
                                        return@onMessage
                                    }
                                }
                            subscribers[ctx]!![channel] = filters
                            sendEvents(channel, filters, ctx)
                            ctx.send("""["EOSE","$channel"]""")
                        }
                        "EVENT" -> {
                            val event = Event.fromJson(jsonArray[1].asString)
                            val rawEvent = event.toJson()
                            println("WS received kind ${event.kind} event.")
                            processEvent(event, rawEvent, ctx)
                        }
                        "CLOSE" -> {
                            val channel = jsonArray[1].asString
                            subscribers[ctx]!!.remove(channel)
                            ctx.send("""["NOTICE","Channel $channel closed."]""")
                        }
                        else -> ctx.send("""["NOTICE","Could not handle $cmd"]""")
                    }
                } catch (e: JsonSyntaxException) {
                    ctx.send("""["NOTICE","No valid JSON: ${gson.toJson(msg)}"]""")
                }
            }
            ws.onClose { ctx ->
                subscribers.remove(ctx)
            }
        }
    }.start("127.0.0.1", 7070)
    // get some recent and all future Events from other relays
    Client.subscribe(object : Client.Listener() {
        override fun onNewEvent(event: Event) {
            processEvent(event, event.toJson())
        }

        override fun onRelayStateChange(type: Relay.Type, relay: Relay) {
            println("${relay.url}: ${type.name}")
        }
    })
    Client.connect(mutableListOf(Filter(since = Calendar.getInstance().apply {
        add(Calendar.HOUR, -24)
    }.time)))
}

private fun sendEvents(channel: String, filters: List<Filter>, ctx: WsContext) {
    val rawEvents = mutableSetOf<String>()
    transaction {
        filters.forEach { filter ->
            val query = Events.select { Events.hidden eq false }.orderBy(createdAt to SortOrder.DESC)
            filter.ids?.let { query.andWhere { hash inList it } }
            filter.kinds?.let { query.andWhere { kind inList it } }
            filter.authors?.let { query.andWhere { pubKey inList it } }
            filter.since?.let { query.andWhere { createdAt greaterEq it } }
            filter.until?.let { query.andWhere { createdAt lessEq it } }
            filter.tags?.let {
                query.adjustColumnSet { innerJoin(Tags, { Events.id }, { event }) }
                it.forEach { query.andWhere { (Tags.key eq it.key) and (Tags.value inList it.value) } }
            }
            filter.limit?.let { query.limit(it) }
            val raws = query.map { it[raw] }
            rawEvents.addAll(raws)
        }
    }
    val t = System.currentTimeMillis()
    rawEvents.forEach {
        ctx.send("""["EVENT","$channel",$it]""")
    }
    println("${rawEvents.size} Events sent in ${System.currentTimeMillis() - t}ms.")
    ctx.send("""["NOTICE","${rawEvents.size} Events sent in ${System.currentTimeMillis() - t}ms."]""")
}

private fun processEvent(e: Event, eventJson: String, sender: WsMessageContext? = null): Boolean {
    // a bit hacky: Make sure to get our clients' events (having a sender) to other relays  ...
    sender?.let { Client.send(e) }
    // ephemeral events get sent and forgotten
    if (e.kind in 20_000..29_999) {
        return forward(e, eventJson, sender)
    }
    // forward if storing succeeds
    return store(e, eventJson, sender) && forward(e, eventJson, sender)
}

private fun store(
    e: Event,
    eventJson: String,
    sender: WsMessageContext?
): Boolean = transaction {
    try {
        if (!DbEvent.find { hash eq e.id.toHex() }.empty()) {
            return@transaction false
        }
        e.checkSignature()
        DbEvent.new {
            hash = e.id.toHex()
            raw = eventJson
            kind = e.kind
            publicKey = e.pubKey.toHex()
            createdAt = e.createdAt
        }
        if (e.kind in listOf(0, 3) || e.kind in 10_000..19_999) {
            // set all but "last" to "hidden"
            DbEvent.find {
                (pubKey eq e.pubKey.toHex()) and (kind eq e.kind) and (Events.hidden eq false)
            }.forEach { it.hidden = true }
            DbEvent.find {
                (pubKey eq e.pubKey.toHex()) and (kind eq e.kind)
            }.orderBy(createdAt to SortOrder.DESC).first().hidden = false
        }
        e.tags.forEach { list ->
            if (list.size >= 2) {
                DbTag.new {
                    event = DbEvent.find { hash eq e.id.toHex() }.first().id.value
                    key = list[0]
                    value = list[1]
                }
            }
        }
        true
    } catch (ex: Exception) {
        sender?.send("""["NOTICE","Something went wrong with event ${e.id.toHex()}"]""")
        println(ex.message ?: "Something went wrong.")
        false
    }
}

private fun forward(
    event: Event,
    eventJson: String,
    ctx: WsMessageContext?
): Boolean {
    subscribers
        .filter { it.key.sessionId != ctx?.sessionId }
        .forEach { (wsContext, channelFilters) ->
            channelFilters.forEach { (channel, filters) ->
                if (filters.any { it.match(event) }) {
                    wsContext.send("""["EVENT","$channel",$eventJson]""")
                }
            }
        }
    return true
}
