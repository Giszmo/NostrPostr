package nostr.postr.examples

import nostr.postr.Client
import nostr.postr.Client.Listener
import nostr.postr.events.Event
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.JsonObject

class LoadJesterEvents {
    companion object {
        @JvmStatic
        fun main(vararg args: String) {
            val gson: Gson = GsonBuilder().create()
            var count = 0
            val listener = object: Listener() {
                override fun onNewEvent(event: Event) {
                    count++
                    println(gson.fromJson(event.content, JsonObject::class.java))
                }
            }
            Client.subscribe(listener)
            // We request to get only kind 30 events - the kind the Jester Chess client uses
            Client.filter = """{"kinds":[30]}"""
            Client.connect()

            Thread.sleep(5_000)
            Client.unsubscribe(listener)
            Client.disconnect()
            println("$count messages received.")
        }
    }
}