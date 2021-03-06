package nostr.relay

import com.google.gson.*
import com.google.gson.reflect.TypeToken
import io.javalin.Javalin
import io.javalin.http.staticfiles.Location
import io.javalin.websocket.WsContext
import io.javalin.websocket.WsMessageContext
import nostr.postr.*
import nostr.postr.events.Event
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

/**
 * Per socket there can be multiple channels with multiple filters each.
 */
val subscribers = mutableMapOf<WsContext, MutableMap<String, List<Filter>>>()
val featureList = mapOf(
    "id" to "wss://relay.nostr.info",
    "name" to "NostrPostrRelay",
    "description" to "Relay running NostrPostr by https://nostr.info",
    "pubkey" to "46fcbe3065eaf1ae7811465924e48923363ff3f526bd6f73d7c184b16bd8ce4d",
    "supported_nips" to listOf(1, 2, 9, 11, 12, 15, 16),
    "software" to "https://github.com/Giszmo/NostrPostr",
    "version" to "0"
)

class NostrRelay

val config: Map<String, String> = NostrRelay::class.java.getResource("/config/local.config.json")
    ?.readText()
    ?.run {
        gson.fromJson(this, object: TypeToken<Map<String, String>>() {}.type)
    }
    ?: mapOf( // default configuration
        "db" to "sqlite",
        "url" to "jdbc:sqlite:events.db",
        "driver" to "org.sqlite.JDBC",
        "fullSync" to "false"
    )

fun main() {
    val rt = Runtime.getRuntime()
    when (config["db"]) {
        "postgresql" -> {
            Database.connect(
                url = config["pg_url"]!!,
                driver = config["pg_driver"]!!,
                user = config["pg_user"]!!,
                password = config["pg_password"]!!)
        }
        else -> {
            Database.connect(
                url = config["lite_url"]!!,
                driver = config["lite_url"]!!,
            )
            TransactionManager.manager.defaultIsolationLevel = Connection.TRANSACTION_SERIALIZABLE
            // or Connection.TRANSACTION_READ_UNCOMMITTED
        }
    }

    transaction {
        addLogger(StdOutSqlLogger)
        SchemaUtils.createMissingTablesAndColumns(Events, Tags)
    }
    Javalin.create {
        it.maxRequestSize = 1 * 1024 * 1024
        it.asyncRequestTimeout = 5L * 60L * 60L * 1_000L
        it.wsFactoryConfig {
            it.policy.maxTextMessageSize = 10 * 1024 * 1024
        }
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
                                .mapIndexedNotNull { index, it ->
                                    try {
                                        JsonFilter.fromJson(it.asJsonObject)
                                    } catch (e: Exception) {
                                        ctx.send("""["NOTICE","Something went wrong with filter $index on channel $channel. Ignoring. Was it a NoMatch?"]""")
                                        println("Something went wrong with filter $index.\n${it}")
                                        null // ignore just this query
                                    }
                                }
                            subscribers[ctx]!![channel] = filters.map { it.spaceOptimized() }
                            sendEvents(channel, filters, ctx)
                            ctx.send("""["EOSE","$channel"]""")
                        }
                        "EVENT" -> {
                            try {
                                val eventJson = jsonArray[1].asJsonObject
                                val event = Event.fromJson(eventJson)
                                println("WS received kind ${event.kind} event. $eventJson")
                                processEvent(event, event.toJson(), ctx)
                            } catch (e: Exception) {
                                println("Something went wrong with Event: ${gson.toJson(jsonArray[1])}")
                            }
                        }
                        "CLOSE" -> {
                            val channel = jsonArray[1].asString
                            subscribers[ctx]!!.remove(channel)
                            println("Channel $channel closed.")
                        }
                        else -> ctx.send("""["NOTICE","Could not handle $cmd"]""")
                    }
                } catch (e: JsonSyntaxException) {
                    ctx.send("""["NOTICE","No valid JSON: ${gson.toJson(msg)}"]""")
                } catch (e: Exception) {
                    ctx.send("""["NOTICE","Exceptions were thrown: ${gson.toJson(msg)}"]""")
                    println(e.message)
                }
            }
            ws.onClose { ctx ->
                println("Session closing. ${ctx.reason()}")
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
    val filter = if(config["fullSync"] == "true") {
        JsonFilter()
    } else {
        JsonFilter(since = Calendar.getInstance().apply { add(Calendar.HOUR, -24) }.time.time / 1000)
    }
    Client.connect(mutableListOf(filter))
    while (true) {
        subscribers.forEach {it.key.sendPing()}
        val queries = subscribers
            .values
            .flatMap { it.values }
            .flatten()
            .map { it.toString() }
        val channelCount = subscribers
            .values
            .count()
        val queryUse = queries
            .distinct()
            .map { it to Collections.frequency(queries, it) }
            .sortedBy { - it.second }
            .joinToString("\n") { "${it.second} times ${it.first}" }
        println("${Date()}: pinging all sockets. ${rt.freeMemory() / 1024 / 1024}MB / ${rt.totalMemory() / 1024 / 1024}MB free. " +
                "${subscribers.size} subscribers maintain $channelCount channels and are monitoring these queries:\n$queryUse")
        Thread.sleep(20_000)
    }
}

private fun sendEvents(channel: String, filters: List<JsonFilter>, ctx: WsContext) {
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
            rawEvents.addAll(query.map { it[raw] })
        }
    }
    val t = System.currentTimeMillis()
    rawEvents.forEach {
        ctx.send("""["EVENT","$channel",$it]""")
    }
    println("${rawEvents.size} Events sent in ${System.currentTimeMillis() - t}ms.")
}

private fun processEvent(e: Event, eventJson: String, sender: WsMessageContext? = null): Boolean {
    e.checkSignature()
    // a bit hacky: Make sure to get our clients' events (having a sender) to other relays  ...
    sender?.let { Client.send(e) }
    // ephemeral events get sent and forgotten
    if (e.kind in 20_000..29_999) {
        return forward(e, eventJson, sender)
    }
    return store(e, eventJson)
            // forward if storing succeeds
            && forward(e, eventJson, sender)
}

private fun store(
    e: Event,
    eventJson: String
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
        println("Something went wrong with event ${e.id.toHex()}: ${ex.message}")
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
