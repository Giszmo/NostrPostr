package nostr.relay

import com.google.gson.GsonBuilder
import com.google.gson.JsonArray
import com.google.gson.JsonSyntaxException
import io.javalin.Javalin
import io.javalin.http.staticfiles.Location
import io.javalin.websocket.WsContext
import nostr.postr.Filter
import nostr.postr.events.Event
import org.eclipse.jetty.util.Loader.getResource

val gson = GsonBuilder().create()
class MyClass
val events = MyClass::class.java.getResource("/event_kind_all.txt")!!
    .readText()
    .split("\n")
    .map { Event.Companion.fromJson(it) }


fun main() {
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

val WsContext.docId: String get() = this.pathParam("doc-id")
