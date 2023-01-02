package nostr.postr.examples

import nostr.postr.*
import nostr.postr.events.Event
import nostr.postr.events.MetadataEvent

/**
 * Get a user's meta data
 */
class LoadOneUserProfile {
    companion object {
        private val pubKey = "46fcbe3065eaf1ae7811465924e48923363ff3f526bd6f73d7c184b16bd8ce4d"
        private val listener = object: Client.Listener() {
            override fun onNewEvent(event: Event) {
                println("From onNewEvent -->")
                if (event.pubKey.toHex() == pubKey) {
                    (event as? MetadataEvent)?.contactMetaData?.run {
                        logDetail(
                            event,
                            "received name: ${name.trim()}, about: ${about.trim()}, nip05: ${nip05?.trim()}, picture: ${picture.trim()}"
                        )
                        stop()
                    }
                } else {
                    logDetail(event, "Why do we get this event? ${event.id}")
                }
            }

            override fun onRelayStateChange(type: Relay.Type, relay: Relay) {
                println("Relay state change -> ${type.name} from ${relay.url}")
                if (type == Relay.Type.EOSE){
                    println("Disconnecting from ${relay.url}")
                    relay.disconnect()
                    println("Disconnected from ${relay.url}")

                }
            }

            override fun onError(error: Error, relay: Relay) {
                println("Error from ${relay.url} : ${error.message}")
                println("Disconnecting from ${relay.url}")
                relay.disconnect()
                println("Disconnected from ${relay.url}")
            }
        }

        @JvmStatic
        fun main(vararg args: String) {
            Client.subscribe(listener)

            Client.connect(subscriptionId = "profile",
                filters = mutableListOf(JsonFilter(kinds = listOf(MetadataEvent.kind), authors = listOf(pubKey)))
            )
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