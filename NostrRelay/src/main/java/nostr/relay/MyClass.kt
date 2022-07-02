package nostr.relay

import com.google.gson.GsonBuilder
import com.google.gson.JsonArray
import com.google.gson.JsonSyntaxException
import io.javalin.Javalin
import io.javalin.http.staticfiles.Location
import io.javalin.websocket.WsContext
import nostr.postr.Filter
import nostr.postr.events.Event
import nostr.postr.events.MetadataEvent
import nostr.postr.toHex
import nostr.relay.Events2Events.references
import org.eclipse.jetty.util.Loader.getResource
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.TransactionManager
import org.jetbrains.exposed.sql.transactions.transaction
import java.lang.Exception
import java.sql.Connection

val gson = GsonBuilder().create()
val events = mutableListOf<Event>()

object Events : Table() {
    val id = char("id", 64)
    val pubKey = char("pubKey", 64) references Personas.pubKey
    val kind = integer("kind")
    val raw = text("raw")

    override val primaryKey = PrimaryKey(id)
}

object Personas: Table() {
    val pubKey = char("id", 64)
    val event = char("event", length = 64) references Events.id
    val name = varchar("name", length = 128)
    val picture = varchar("picture", length = 256).nullable()
    val about = text("about")
    val nip05 = varchar("nip05", length = 256).nullable()

    override val primaryKey = PrimaryKey(pubKey)
}

object Follows: Table() {
    val from = char("from", 64).index() references Personas.pubKey
    val to = char("to", 64) references Personas.pubKey

    override val primaryKey = PrimaryKey(from, to)
}

object Events2Events: Table() {
    val from = char("from", 64).index() references Events.id
    val to = char("to", 64) references Events.id
    val type = integer("type") // if "from" references multiple "to"s, the index is stored here.

    override val primaryKey = PrimaryKey(from, to)
}

object Events2Personas: Table() {
    val from = char("from", 64).index() references Events.id
    val to = char("to", 64) references Personas.pubKey

    override val primaryKey = PrimaryKey(from, to)
}

object Tags: Table() {
    val event = char("event_id", 64) references Events.id
}

fun main() {
    Database.connect("jdbc:sqlite:events.db", "org.sqlite.JDBC")
    TransactionManager.manager.defaultIsolationLevel =
        Connection.TRANSACTION_SERIALIZABLE
        // or Connection.TRANSACTION_READ_UNCOMMITTED
    transaction {
        addLogger(StdOutSqlLogger)
        SchemaUtils.create(Events)
        events.addAll(
            Events.selectAll().map { Event.fromJson(it[Events.raw]) }
        )
    }
    val subscribers = mutableMapOf<WsContext, MutableMap<String, List<Filter>>>()
    Javalin.create {
        it.addStaticFiles("/public", Location.CLASSPATH)
    }.apply {
        ws("/ws") { ws ->
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
                                .map {
                                    Filter.fromJson(it.asJsonObject)
                                }
                            subscribers[ctx]!![channel] = filters
                            events
                                .filter { ev -> filters.any {
                                    it.match(ev) } }
                                .forEach { ctx.send("""["EVENT","$channel",${it.toJson()}]""") }
                            ctx.send("""["EOSE","$channel"]""")
                        }
                        "EVENT" -> {
                            val event = Event.fromJson(jsonArray[1])
                            if (events.any { it.id.toHex() == event.id.toHex() }) {
                                ctx.send("""["NOTICE","Already had eventId ${event.id.toHex()}"]""")
                                return@onMessage
                            }
                            val rawEvent = event.toJson()
                            addEvent(event, rawEvent)
                            subscribers
                                .filter { it.key.sessionId != ctx.sessionId }
                                .forEach { (wsContext, channelFilters) ->
                                    channelFilters.forEach { (channel, filters) ->
                                        if (filters.any { it.match(event) }) {
                                            wsContext.send("""["EVENT","$channel",$rawEvent]""")
                                        }
                                    }
                                }
                        }
                        "CLOSE" -> {
                            val channel = jsonArray[1].asString
                            subscribers[ctx]!!.remove(channel)
                            ctx.send("""["NOTICE","Channel $channel closed."]""")
                        }
                        else -> ctx.send("""["NOTICE","Could not handle $cmd"]""")
                    }
                } catch(e: JsonSyntaxException) {
                    ctx.send("""["NOTICE","No valid JSON: ${gson.toJson(msg)}"]""")
                }
            }
            ws.onClose { ctx ->
                subscribers.remove(ctx)
            }
        }
    }.start(7070)
}

fun addEvent(event: Event, eventJson: String? = null) {
    transaction {
        try {
            event.checkSignature()
            Events.insert {
                it[id] = event.id.toHex()
                it[raw] = eventJson ?: event.toJson()
                it[kind] = event.kind
                it[pubKey] = event.pubKey.toHex()
            }
            events.add(0, event)
        } catch (e: Exception) {
            println(e.message ?: "Something went wrong.")
        }
    }
}

val WsContext.docId: String get() = this.pathParam("doc-id")
