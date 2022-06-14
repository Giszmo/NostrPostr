package nostr.postr

import com.google.gson.JsonElement
import nostr.postr.events.Event
import okhttp3.*

class Relay(
    val url: String,
    var read: Boolean,
    var write: Boolean
) {
    private val httpClient = OkHttpClient()
    private val listeners = HashSet<Listener>()
    private lateinit var socket: WebSocket

    fun register(listener: Listener) {
        listeners.add(listener)
    }

    fun unregister(listener: Listener) = listeners.remove(listener)

    fun connect() {
        val request = Request.Builder().url(url).build()
        val listener = object : WebSocketListener() {
            // private val NORMAL_CLOSURE_STATUS: Int = 1000
            override fun onOpen(webSocket: WebSocket, response: Response) {
                webSocket.send("""["REQ","main-channel",${Client.filters.joinToString()}]""")
                listeners.forEach { it.onRelayStateChange(this@Relay, TYPE_CONNECT) }
                // webSocket.close(NORMAL_CLOSURE_STATUS, "Goodbye!")
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                try {
                    val msg = Event.gson.fromJson(text, JsonElement::class.java).asJsonArray
                    val type = msg[0].asString
                    val channel = msg[1].asString
                    if (type == "EVENT") {
                        val event = Event.fromJson(msg[2])
                        listeners.forEach { it.onEvent(this@Relay, event) }
                    } else {
                        listeners.forEach {
                            it.onError(
                                this@Relay,
                                Error("Unknown type $type on channel $channel. Msg was $text")
                            )
                        }
                    }
                } catch (t: Throwable) {
                    text.chunked(2000) { chunked ->
                        listeners.forEach { it.onError(this@Relay, Error("Problem with $chunked")) }
                    }
                }
            }

            override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
                listeners.forEach { it.onRelayStateChange(this@Relay, TYPE_DISCONNECT) }
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                listeners.forEach {
                    it.onError(this@Relay, Error("WebSocket Failure", t))
                }
            }
        }
        socket = httpClient.newWebSocket(request, listener)
    }

    fun disconnect() {
        httpClient.dispatcher.executorService.shutdown()
        socket.close(1000, "Normal close")
    }

    fun sendFilter() {
        // TODO: this results in the reception of many message duplicates!
        val request = """["REQ","main-channel",${Client.filters.joinToString(",")}]"""
        socket.send(request)
    }

    companion object {
        const val TYPE_DISCONNECT = 0
        const val TYPE_CONNECT = 1

        private var channelCounter = 0
    }

    interface Listener {
        /**
         * A new message was received
         */

        fun onEvent(relay: Relay, event: Event)

        fun onError(relay: Relay, error: Error)

        /**
         * Connected to or disconnected from a relay
         *
         * @param type is 0 for disconnect and 1 for connect
         */
        fun onRelayStateChange(relay: Relay, type: Int)
    }
}
