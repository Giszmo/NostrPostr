package nostr.postr.examples

import nostr.postr.Client
import nostr.postr.Client.Listener
import nostr.postr.events.Event
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.JsonObject
import nostr.postr.Constants
import nostr.postr.Relay
import nostr.postr.events.ContactListEvent
import nostr.postr.events.MetadataEvent
import nostr.postr.events.TextNoteEvent
import nostr.postr.toHex
import java.text.SimpleDateFormat
import java.util.*

/**
 * Get all Text Notes according to [nip 1](https://github.com/nostr-protocol/nips/blob/master/01.md)
 *
 * This example prints out the first 1000 short and simple messages received.
 */
class LoadShortSimpleTextNotes {
    companion object {
        private var count = 0
        private val listener = object: Listener() {
            override fun onNewEvent(event: Event) {
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
            // We request to get only kind 30 events - the kind the Jester Chess client uses
            val filters = arrayOf("""{"kinds":[1]}""")
            Client.connect(filters)
        }

        fun stop() {
            Client.unsubscribe(listener)
            Client.disconnect()
        }
    }
}

/**
 * Get a user's meta data
 */
class LoadOneUserProfile {
    companion object {
        private val pubKey = "46fcbe3065eaf1ae7811465924e48923363ff3f526bd6f73d7c184b16bd8ce4d"
        private val listener = object: Listener() {
            override fun onNewEvent(event: Event) {
                if (event.pubKey.toHex() == pubKey) {
                    (event as? MetadataEvent)?.contactMetaData?.run {
                        logDetail(event, "received name: ${name.trim()}, about: ${about.trim()}, nip05: ${nip05?.trim()}, picture: ${picture.trim()}")
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
            // We request to get only kind 30 events - the kind the Jester Chess client uses
            val filters = arrayOf("""{"kinds":[${MetadataEvent.kind}],"authors":["$pubKey"]}""")
            Client.connect(filters)
        }

        fun stop() {
            Client.unsubscribe(listener)
            Client.disconnect()
        }
    }
}

/**
 * Get a user's meta data
 */
class LoadOneContactListTrackingRelays {
    companion object {
        val startTime = System.currentTimeMillis()
        private val pubKey = "46fcbe3065eaf1ae7811465924e48923363ff3f526bd6f73d7c184b16bd8ce4d"
        private val listener = object: Listener() {
            override fun onEvent(event: Event, relay: Relay) {
                if (event.pubKey.toHex() == pubKey) {
                    logDetail(event, "Time elapsed ${(System.currentTimeMillis() - startTime)/1000f}s for ${relay.url}")
                } else {
                    logDetail(event, "Why do we get this event from ${relay.url}?")
                }
            }
        }

        @JvmStatic
        fun main(vararg args: String) {
            println("Requesting Contact List from ${Constants.defaultRelays.size} relays, measuring time for 10s ...")
            Client.subscribe(listener)
            // We request to get only kind 30 events - the kind the Jester Chess client uses
            val filters = arrayOf("""{"kinds":[${ContactListEvent.kind}],"authors":["$pubKey"]}""")
            Client.connect(filters)
            Thread.sleep(3_000)
            stop()
        }

        fun stop() {
            Client.unsubscribe(listener)
            Client.disconnect()
        }
    }
}

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
        private val listener = object: Listener() {
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
            val filters = arrayOf("""{"kinds":[30]}""")
            Client.connect(filters)
        }

        fun stop() {
            Client.unsubscribe(listener)
            Client.disconnect()
        }
   }
}

// helpers
val dateTimeFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)
fun Event.userShort() = pubKey.toHex().substring(0, 8)
fun Event.prettyDate() = dateTimeFormat.format(Date(createdAt * 1000))
fun logDetail(event: Event, detail: String) = println("${event.prettyDate()} ${event.userShort()}: $detail")