package nostr.postr.examples

import nostr.postr.Client
import nostr.postr.Client.Listener
import nostr.postr.Relay
import nostr.postr.events.Event
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.JsonObject

class LoadJesterEvents {
    companion object {
        @JvmStatic
        fun main(vararg args: String) {
            val gson: Gson = GsonBuilder().create()
            Client.subscribe(object: Listener {
                override fun onEvent(event: Event, relay: Relay) {
                    println(gson.fromJson(event.content, JsonObject::class.java))
                }

                override fun onNewEvent(event: Event) {}

                override fun onError(error: Error, relay: Relay) {}

                override fun onRelayStateChange(type: Int, relay: Relay) {}
            })
            Client.connect()

        }
    }
}