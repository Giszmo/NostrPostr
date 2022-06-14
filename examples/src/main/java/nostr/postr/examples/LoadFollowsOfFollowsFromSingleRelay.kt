package nostr.postr.examples

import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import nostr.postr.Client
import nostr.postr.Relay
import nostr.postr.events.ContactListEvent
import nostr.postr.events.Event
import nostr.postr.events.MetadataEvent
import nostr.postr.toHex

class LoadFollowsOfFollowsFromSingleRelay {
    companion object {
        private val relays = arrayOf(
            Relay("wss://nostr-relay.untethr.me", read = true, write = true)
        )
        private val pubKey = "46fcbe3065eaf1ae7811465924e48923363ff3f526bd6f73d7c184b16bd8ce4d"
        private val follows: MutableSet<String> = mutableSetOf()
        private val followsOfFollows: MutableSet<String> = mutableSetOf()
        private val followsOfFollowsOfFollows: MutableSet<String> = mutableSetOf()
        private var followsReceived = 0
        private var followsOfFollowsReceived = 0
        private var metadataReceived = 0
        private val keyNames = mutableMapOf<String, String>()
        private var eventsReceived = mutableListOf<String>()
        private val listener = object: Client.Listener() {
            override fun onNewEvent(event: Event) {
                when (event) {
                    is ContactListEvent -> onContactList(event)
                    is MetadataEvent -> onMetadataEvent(event)
                }
            }

            override fun onEvent(event: Event, relay: Relay) {
                eventsReceived.add("${event.pubKey.toHex().substring(0, 6)}_${event.kind}")
            }
        }

        private fun onContactList(event: ContactListEvent) {
            if (event.pubKey.toHex() == pubKey) {
                follows.addAll(event.follows.map { it.pubKeyHex })
                val filter = """{"kinds":[${ContactListEvent.kind}],"authors":[${follows.joinToString(",") { "\"$it\"" }}]}"""
                Client.addFilter(filter)
                println("Phase two: Requesting user's follows' follows")
            } else if (event.pubKey.toHex() in follows) {
                followsOfFollows.addAll(event.follows.map { it.pubKeyHex })
                followsReceived++
                GlobalScope.launch {
                    val x = followsReceived
                    delay(2_000)
                    if (x == followsReceived) {
                        val filter =
                            """{"kinds":[${ContactListEvent.kind}],"authors":[${followsOfFollows.joinToString(",") { "\"$it\"" }}]}"""
                        Client.addFilter(filter)
                        println("Phase three: Requesting user's follows' follows' follows")
                    }
                }
            } else if (event.pubKey.toHex() in followsOfFollows) {
                followsOfFollowsOfFollows.addAll(event.follows.map { it.pubKeyHex })
                followsOfFollowsReceived ++
                GlobalScope.launch {
                    val x = followsOfFollowsReceived
                    delay(2_000)
                    if (x == followsOfFollowsReceived) {
                        val filter =
                            """{"kinds":[${MetadataEvent.kind}],"authors":[${(followsOfFollowsOfFollows + followsOfFollows + follows + pubKey).joinToString(",") { "\"$it\"" }}]}"""
                        Client.addFilter(filter)
                        println("Phase four: Requesting everybody's names")
                    }
                }
            } else {
                logDetail(event, "this was unexpected.")
            }
        }

        private fun onMetadataEvent(event: MetadataEvent) {
            keyNames[event.pubKey.toHex()] = event.contactMetaData.name
            metadataReceived++
            GlobalScope.launch {
                val x = metadataReceived
                delay(2_000)
                if (x == metadataReceived) {
                    stop()
                    println("Phase five: Results\n")
                    println("Suspect Zero:")
                    println("f0: ${pubKey} ${keyNames[pubKey]}")
                    println("Following these ${follows.size}:")
                    follows.forEach {
                        println("f1: ${it} ${keyNames[it]}")
                    }
                    println("Following these further (circular follows removed) ${followsOfFollows.size}:")
                    followsOfFollows.forEach {
                        println("f2: ${it} ${keyNames[it]}")
                    }
                    println("Following these further (circular follows removed) ${followsOfFollowsOfFollows.size}:")
                    followsOfFollowsOfFollows.forEach {
                        println("f3: ${it} ${keyNames[it]}")
                    }
                    println("\nTotal Events received: ${eventsReceived.size}")

                    println("TODO: Why are so many of these events received so many times?")
                    println(eventsReceived.joinToString(","))
                }
            }
        }

        @JvmStatic
        fun main(vararg args: String) {
            Client.subscribe(listener)
            println("Phase one: Requesting user's follows")
            val filters = mutableListOf("""{"kinds":[${ContactListEvent.kind}],"authors":["$pubKey"]}""")
            Client.connect(filters, relays)
        }

        private fun stop() {
            Client.unsubscribe(listener)
            Client.disconnect()
        }
    }
}