package nostr.postr

import okhttp3.*
import com.google.gson.GsonBuilder
import com.google.gson.JsonElement

class Relay(
    val url: String,
    var canRead: Boolean,
    var canWrite: Boolean
) {
    private var httpClient = OkHttpClient()
    private var gson = GsonBuilder().create()
    private val listeners = HashSet<Listener>()

    fun register(listener: Listener) {
        listeners.add(listener)
    }

    fun unregister(listener: Listener) = listeners.remove(listener)

    fun connect() {
        val request = Request.Builder().url(url).build()
        val listener = object : WebSocketListener() {
            // private val NORMAL_CLOSURE_STATUS: Int = 1000
            override fun onOpen(webSocket: WebSocket, response: Response) {
                webSocket.send("""["REQ","main-channel",{"kinds":[0,1,2,3],"authors":["8c0da4862130283ff9e67d889df264177a508974e2feb96de139804ea66d6168","22e804d26ed16b68db5259e78449e96dab5d464c8f470bda3eb1a70467f2c793","e05593eb83c75636a1bfdaf8069abc2acad4dbb39dcd828f1c6cf4fbdb6043b9","6b0d4c8d9dc59e110d380b0429a02891f1341a0fa2ba1b1cf83a3db4d47e3964","d4d4fdde8ab4924b1e452e896709a3bd236da4c0576274b52af5992d4d34762c","ad5aab5be883a571ea37b231cd996d37522e77d0f121cedfd6787b91d848268e","e6a92d8b6c20426f78bba8510ccdc73df5122814a3bac1d553adebac67a92b27","c2bb5d6529095edbfbdbe3f136175c146c6706526325b32da881c7c34c7b1ab8","b2d670de53b27691c0c3400225b65c35a26d06093bcc41f48ffc71e0907f9d4a","e9e4276490374a0daf7759fd5f475deff6ffb9b0fc5fa98c902b5f4b2fe3bba2","76f928b303b095a6f17784151acd9a5127d183cb5f989a173b00bd0c12d07e83","32e1827635450ebb3c5a7d12c1f8e7b2b514439ac10a67eef3d9fd9c5c68e245","2ef93f01cd2493e04235a6b87b10d3c4a74e2a7eb7c3caf168268f6af73314b5","46fcbe3065eaf1ae7811465924e48923363ff3f526bd6f73d7c184b16bd8ce4d"]},{"kinds":[1,4],"#p":["46fcbe3065eaf1ae7811465924e48923363ff3f526bd6f73d7c184b16bd8ce4d"]},{"kinds":[4],"authors":["46fcbe3065eaf1ae7811465924e48923363ff3f526bd6f73d7c184b16bd8ce4d"]}]""")
                listeners.forEach { it.onRelayStateChange(this@Relay, 1) }
                // webSocket.close(NORMAL_CLOSURE_STATUS, "Goodbye!")
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                val msg = gson.fromJson(text, JsonElement::class.java).asJsonArray
                val type = msg[0].asString
                val channel = msg[1].asString
                val eventJson = msg[2].asString
                if (type == "EVENT") {
                    val event = Event.fromJson(eventJson)
                    listeners.forEach { it.onEvent(this@Relay, event) }
                } else {
                    listeners.forEach {
                        it.onError(this@Relay,
                            Error("Unknown type $type on channel $channel."))
                    }
                }
            }

            override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
                listeners.forEach { it.onRelayStateChange(this@Relay, 0) }
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                listeners.forEach {
                    it.onError(this@Relay, Error("WebSocket Failure", t))
                }
            }
        }
        httpClient.newWebSocket(request, listener)
    }

    fun disconnect() {
         httpClient.dispatcher.executorService.shutdown()
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