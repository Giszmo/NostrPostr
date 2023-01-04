package nostr.postr.examples

import nostr.postr.Client
import nostr.postr.JsonFilter
import nostr.postr.events.Event
import nostr.postr.events.TextNoteEvent

/**
 * Get all Text Notes according to [nip 1](https://github.com/nostr-protocol/nips/blob/master/01.md)
 *
 * This example prints out the first 1000 short and simple messages received.
 */
class LoadShortSimpleTextNotes {
    companion object {
        private var count = 0
        private val listener = object: Client.Listener() {
            override fun onNewEvent(event: Event, subscriptionId: String) {
                (event as? TextNoteEvent)?.run {
                    // only match short, simple messages with no markdown, html or other fancy stuff
                    val pattern = Regex("[a-zA-Z .,:!?]{2,30}")
                    if (content.matches(pattern)) {
                        count++
                        logDetail(event, content)
                        if (count == 1000) {
                            stop()
                        }
                    }
                }
            }
        }

        @JvmStatic
        fun main(vararg args: String) {
            Client.subscribe(listener)
            Client.connect()
            Client.requestAndWatch(filters = mutableListOf(JsonFilter(kinds = listOf(TextNoteEvent.kind))))
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