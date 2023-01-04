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
            override fun onNewEvent(event: Event, subscriptionId: String) {
                println("From onNewEvent with subscription ID $subscriptionId -->")
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

        }

        @JvmStatic
        fun main(vararg args: String) {
            Client.subscribe(listener)
            Client.connect()
            Client.request(subscriptionId = "profile",
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