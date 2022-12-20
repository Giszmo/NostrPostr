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
import nostr.relay.Events.dTag
import nostr.relay.Events.hash
import nostr.relay.Events.kind
import nostr.relay.Events.pubKey
import nostr.relay.Events.raw
import org.jetbrains.exposed.exceptions.ExposedSQLException
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.TransactionManager
import org.jetbrains.exposed.sql.transactions.transaction
import java.security.InvalidParameterException
import java.sql.Connection
import java.util.*

val gson: Gson = GsonBuilder().create()

/**
 * Per socket there can be multiple channels with multiple filters each.
 */
val subscribers: MutableMap<WsContext, MutableMap<String, List<Filter>>> = Collections.synchronizedMap(LinkedHashMap())
val featureList = mapOf(
    "id" to "wss://relay.nostr.info",
    "name" to "NostrPostrRelay",
    "description" to "Relay running NostrPostr by https://nostr.info",
    "pubkey" to "46fcbe3065eaf1ae7811465924e48923363ff3f526bd6f73d7c184b16bd8ce4d",
    "supported_nips" to listOf(1, 2, 9, 11, 12, 15, 16, 33),
    "software" to "https://github.com/Giszmo/NostrPostr",
    "version" to "1"
)

var eventTiming = 0 to 0
var channelCloseCounter = 0
var sessionCloseCounter = 0
var eventReceived = 0

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
        it.enableCorsForAllOrigins()
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
            ws.onClose { ctx ->
                sessionCloseCounter++
                subscribers.remove(ctx)
            }
            ws.onError { ctx ->
                val error = ctx.error()?.message ?: "unknown"
                println("ws.onError(${ctx.error()})")
                ctx.closeSession(2, "Error received: $error")
                subscribers.remove(ctx)
            }
            ws.onMessage { ctx ->
                val msg = ctx.message()
                try {
                    val jsonArray = gson.fromJson(msg, JsonArray::class.java)
                    when (val cmd = jsonArray[0].asString ?: "") {
                        "REQ" -> onRequest(jsonArray, ctx)
                        "EVENT" -> onEvent(jsonArray, ctx)
                        "CLOSE" -> onClose(jsonArray, ctx)
                        "RID" -> ctx.send("""["RID",${gson.toJson(featureList)}]""")
                        else -> onUnknown(ctx, cmd, msg)
                    }
                } catch (e: JsonSyntaxException) {
                    ctx.send("""["NOTICE","No valid JSON: ${gson.toJson(msg)}"]""")
                } catch (e: Exception) {
                    ctx.send("""["NOTICE","Exceptions were thrown: ${gson.toJson(msg)}"]""")
                    println("Exception on ws message:")
                    e.printStackTrace()
                }
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
        // to cover even extended down-times automatically
        JsonFilter(since = Calendar.getInstance().apply { add(Calendar.HOUR, -6) }.time.time / 1000)
    }
    Client.connect(mutableListOf(filter))
    while (true) {
        subscribers.forEach {it.key.sendPing()}
        val queries = subscribers
            .values
            .flatMap { it.values }
            .flatten()
            .map { it.toShortString() }
        val channelCount = subscribers
            .values
            .count()
        val queryUse = queries
            .distinct()
            .map { it to Collections.frequency(queries, it) }
            .sortedBy { - it.second }
            .joinToString("\n") { "${it.second} times ${it.first}" }
        println("""
            
            ${Date()}: pinging all sockets. ${rt.freeMemory() / 1024 / 1024}MB / ${rt.totalMemory() / 1024 / 1024}MB free.
            ${subscribers.size} subscribers maintain $channelCount channels and are monitoring these queries:
            $queryUse
            ${eventTiming.first} Events sent in ${eventTiming.second}ms.
            $eventReceived Events received via Websocket.
            $channelCloseCounter Channels closed.
            $sessionCloseCounter Sessions closed.
            """.trimIndent())
        eventTiming = 0 to 0
        channelCloseCounter = 0
        sessionCloseCounter = 0
        eventReceived = 0
        Thread.sleep(10_000)
    }
}

private fun onUnknown(ctx: WsMessageContext, cmd: String, msg: String) {
    println("""Received unknown command "$cmd": $msg""")
    ctx.send("""["NOTICE","Could not handle command "$cmd""]""")
}

private fun onClose(
    jsonArray: JsonArray,
    ctx: WsMessageContext
) {
    val channel = jsonArray[1].asString
    subscribers[ctx]!!.remove(channel)
    channelCloseCounter++
}

private fun onEvent(
    jsonArray: JsonArray,
    ctx: WsMessageContext
) {
    try {
        val eventJson = jsonArray[1].asJsonObject
        val event = Event.fromJson(eventJson)
        eventReceived++
        processEvent(event, event.toJson(), ctx)
    } catch (e: Exception) {
        println("Something went wrong with Event: ${gson.toJson(jsonArray[1])}")
        e.printStackTrace()
    }
}

private fun onRequest(
    jsonArray: JsonArray,
    ctx: WsMessageContext
) {
    val channel = jsonArray[1].asString
    val filters = jsonArray
        .filterIndexed { index, _ -> index > 1 }
        .mapIndexedNotNull { index, it ->
            try {
                JsonFilter.fromJson(it.asJsonObject)
            } catch (e: InvalidParameterException) {
                println("Ignoring no-match filter $it")
                null
            } catch (e: Exception) {
                ctx.send("""["NOTICE","Something went wrong with filter $it on channel $channel. Ignoring."]""")
                println("Something went wrong with filter $index. $it")
                null // ignore just this query
            }
        }
    subscribers[ctx]!![channel] = filters.map { it.spaceOptimized() }
    sendEvents(channel, filters, ctx)
    ctx.send("""["EOSE","$channel"]""")
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
    eventTiming = eventTiming.first + rawEvents.size to eventTiming.second + (System.currentTimeMillis() - t).toInt()
}

private fun processEvent(e: Event, eventJson: String, sender: WsMessageContext? = null): Boolean {
    e.checkSignature()
    // a bit hacky: Make sure to get our clients' events (having a sender) to other relays  ...
    sender?.let { Client.send(e) }
    // ephemeral events get sent and forgotten
    if (e.kind in 20_000..29_999) {
        return forward(e, eventJson)
    }
    return store(e, eventJson)
            // forward if storing succeeds
            && forward(e, eventJson)
}

private fun store(
    e: Event,
    eventJson: String
): Boolean = transaction {
    val hexId = e.id.toHex()
    val firstDTag = e.tags.firstOrNull { it.first() == "d" }?.getOrNull(1) ?: ""
    try {
        if (!DbEvent.find { hash eq hexId }.empty()) {
            return@transaction false
        }
        e.checkSignature()
        val hexPubkey = e.pubKey.toHex()
        val dbEvent = DbEvent.new {
            hash = hexId
            raw = eventJson
            kind = e.kind
            publicKey = hexPubkey
            createdAt = e.createdAt
            dTag = if (e.kind in 30_000..39_999) {
                firstDTag
            } else {
                null
            }
        }
        if (e.kind in listOf(0, 3) || e.kind in 10_000..19_999) {
            // set all but "last" to "hidden"
            val events = DbEvent.find { (pubKey eq hexPubkey) and (kind eq e.kind) }
            events.forEach { it.hidden = true }
            // set last to not hidden
            events.orderBy(createdAt to SortOrder.DESC).first().hidden = false
        }
        if (e.kind in 30_000..39_999) {
            // set all but "last" to "hidden" considering the first d-tag as per nip33
            val events = DbEvent.find {
                (pubKey eq hexPubkey) and (kind eq e.kind) and (dTag eq firstDTag)
            }
            events.forEach { it.hidden = true }
            // set last to not hidden
            events.orderBy(createdAt to SortOrder.DESC).first().hidden = false
        }
        e.tags.forEach { list ->
            if (list.size >= 2 && list[0].length <= 20) {
                DbTag.new {
                    event = dbEvent.id.value
                    key = list[0]
                    value = list.getOrNull(1) ?: ""
                }
            }
        }
        true
    } catch (ex: ExposedSQLException) {
        println("Error Code: ${ex.errorCode}")
        false
    } catch (ex: Exception) {
        println("Something went wrong with event $hexId")
        ex.printStackTrace()
        false
    }
}

private fun forward(
    event: Event,
    eventJson: String
): Boolean {
    subscribers
        .forEach { (wsContext, channels) ->
            channels.forEach { (channel, filters) ->
                if (filters.any { it.match(event) }) {
                    wsContext.send("""["EVENT","$channel",$eventJson]""")
                }
            }
        }
    return true
}
