package nostr.postr.examples

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.JsonObject
import nostr.postr.Client
import nostr.postr.Filter
import nostr.postr.events.Event

/**
 * This example deals with nostr events that are non-standard. If you use nostr as messaging
 * protocol and define your format, you can pick any integer as "kind" to work with and start
 * sending and receiving messages. For example Jester is a chess app. As a nostr library it will not
 * implement the rules of chess but you can use the library to send and receive those messages.
 * Jester picked kind 30 and we can easily receive the corresponding messages.
 */
class LoadJesterEvents {
    companion object {
        private val gson: Gson = GsonBuilder().create()
        private var count = 0
        private val listener = object: Client.Listener() {
            override fun onNewEvent(event: Event) {
                count++
                // Jester stores data as JSON in the Event's content. Here we just extract the chess
                // move and print it
                val move = gson.fromJson(event.content, JsonObject::class.java).get("move").asString
                logDetail(event, move)
                if (count == 1000) {
                    stop()
                }
            }
        }

        @JvmStatic
        fun main(vararg args: String) {
            Client.subscribe(listener)
            // We request to get only kind 30 events - the kind the Jester Chess client uses
            Client.connect(mutableListOf(Filter(kinds = listOf(30))))
            while (running) {
                Thread.sleep(100)
            }
        }

        var running = true
        private fun stop() {
            running = false
            Client.unsubscribe(listener)
            Client.disconnect()
        }
   }
}