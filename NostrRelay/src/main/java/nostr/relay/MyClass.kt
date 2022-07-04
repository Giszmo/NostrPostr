package nostr.relay

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.JsonArray
import com.google.gson.JsonSyntaxException
import io.javalin.Javalin
import io.javalin.http.staticfiles.Location
import io.javalin.websocket.WsContext
import nostr.postr.Client
import nostr.postr.Filter
import nostr.postr.events.Event
import nostr.postr.toHex
import nostr.relay.Events.createdAt
import nostr.relay.Events.hash
import nostr.relay.Events.kind
import nostr.relay.Events.pubKey
import nostr.relay.Events.raw
import org.jetbrains.exposed.dao.Entity
import org.jetbrains.exposed.dao.EntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.TransactionManager
import org.jetbrains.exposed.sql.transactions.transaction
import java.sql.Connection

val gson: Gson = GsonBuilder().create()

object Events: IntIdTable() {
    val hash = char("hash", 64).index()
    val pubKey = char("pubKey", 64)
    val kind = integer("kind")
    val raw = text("raw")
    val createdAt = long("createdAt")
    val hidden = bool("hidden").default(false)
    val firstSeen = long("firstSeen").default(System.currentTimeMillis())
}

class DbEvent(id: EntityID<Int>): Entity<Int>(id) {
    companion object: EntityClass<Int, DbEvent>(Events)

    var hash by Events.hash
    var publicKey by pubKey
    var kind by Events.kind
    var raw by Events.raw
    var createdAt by Events.createdAt
    var hidden by Events.hidden
    var firstSeen by Events.firstSeen
}

object Tags : IntIdTable() {
    val event = integer("event_id").references(Events.id)
    val key = varchar("key", length = 20).index()
    val value = text("value").index()
}

class DbTag(id: EntityID<Int>): Entity<Int>(id) {
     companion object: EntityClass<Int, DbTag>(Tags)

    var event by Tags.event
    var key by Tags.key
    var value by Tags.value
}

fun main() {
    Database.connect("jdbc:sqlite:events.db", "org.sqlite.JDBC")
    TransactionManager.manager.defaultIsolationLevel =
        Connection.TRANSACTION_SERIALIZABLE
    // or Connection.TRANSACTION_READ_UNCOMMITTED
    transaction {
        addLogger(StdOutSqlLogger)
        SchemaUtils.createMissingTablesAndColumns(Events, Tags)
    }
    val subscribers = mutableMapOf<WsContext, MutableMap<String, List<Filter>>>()
    Javalin.create {
        it.addStaticFiles("/public", Location.CLASSPATH)
    }.apply {
        ws("/ws") { ws ->
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
                            val event = Event.fromJson(jsonArray[1])
                            val rawEvent = event.toJson()
                            if (event.kind in 20_000..29_999 // ephemeral events get sent and forgotten
                                || addEvent(event, rawEvent)) { // non-ephemeral events get sent and stored
                                subscribers
                                    .filter { it.key.sessionId != ctx.sessionId }
                                    .forEach { (wsContext, channelFilters) ->
                                        channelFilters.forEach { (channel, filters) ->
                                            if (filters.any { it.match(event) }) {
                                                wsContext.send("""["EVENT","$channel",$rawEvent]""")
                                            }
                                        }
                                    }
                            } else {
                                ctx.send("""["NOTICE","Something went wrong with event ${event.id.toHex()}"]""")
                            }
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
    }.start(7070)
    // get 100 random and all future Events from other relays
    Client.subscribe(object : Client.Listener() {
        override fun onNewEvent(event: Event) {
            addEvent(event, event.toJson())
        }
    })
    Client.connect(mutableListOf(Filter(limit = 100)))
}

fun sendEvents(channel: String, filters: List<Filter>, ctx: WsContext) {
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
            query.forEach { rawEvents.add(it[raw]) }
        }
    }
    val t = System.currentTimeMillis()
    rawEvents.forEach {
        ctx.send("""["EVENT","$channel",$it]""")
    }
    println("${rawEvents.size} Events sent in ${System.currentTimeMillis()-t}ms.")
    ctx.send("""["NOTICE","${rawEvents.size} Events sent in ${System.currentTimeMillis()-t}ms."]""")
}

fun addEvent(e: Event, eventJson: String): Boolean = transaction {
    try {
        e.checkSignature()
        DbEvent.new {
            hash = e.id.toHex()
            raw = eventJson
            kind = e.kind
            publicKey = e.pubKey.toHex()
            createdAt = e.createdAt
        }
        if (e.kind == 0 || e.kind in 10_000..19_999) {
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
    } catch (e: Exception) {
        println(e.message ?: "Something went wrong.")
        false
    }
}
