package nostr.postr.examples

import nostr.postr.Client
import nostr.postr.Constants
import nostr.postr.Relay
import nostr.postr.events.ContactListEvent
import nostr.postr.events.Event
import nostr.postr.toHex

/**
 * Get a user's meta data
 */
class LoadOneContactListTrackingRelays {
    companion object {
        val startTime = System.currentTimeMillis()
        private val pubKey = "46fcbe3065eaf1ae7811465924e48923363ff3f526bd6f73d7c184b16bd8ce4d"
        private val listener = object: Client.Listener() {
            override fun onEvent(event: Event, relay: Relay) {
                if (event.pubKey.toHex() == pubKey) {
                    logDetail(
                        event,
                        "Time elapsed ${(System.currentTimeMillis() - startTime) / 1000f}s for ${relay.url}"
                    )
                } else {
                    logDetail(event, "Why do we get this event from ${relay.url}?")
                }
            }
        }

        @JvmStatic
        fun main(vararg args: String) {
            println("Requesting Contact List from ${Constants.defaultRelays.size} relays, measuring time for 10s ...")
            Client.subscribe(listener)
            val filters = mutableListOf("""{"kinds":[${ContactListEvent.kind}],"authors":["$pubKey"]}""")
            Client.connect(filters)
            Thread.sleep(3_000)
            stop()
        }

        private fun stop() {
            Client.unsubscribe(listener)
            Client.disconnect()
        }
    }
}