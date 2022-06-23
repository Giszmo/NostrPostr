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
        private val relays = arrayOf(Relay("wss://nostr-relay.untethr.me", true, false))
        private val pubKey = "46fcbe3065eaf1ae7811465924e48923363ff3f526bd6f73d7c184b16bd8ce4d"
        private val follows: Array<MutableSet<String>> = arrayOf(mutableSetOf(), mutableSetOf(), mutableSetOf())
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
                follows[0].addAll(event.follows.map { it.pubKeyHex })
                val filter = """{"kinds":[${ContactListEvent.kind}],"authors":[${follows[0].joinToString(",") { "\"$it\"" }}]}"""
                Client.addFilter(filter)
                println("Phase two: Requesting user's follows' follows")
            } else if (event.pubKey.toHex() in follows[0]) {
                follows[1].addAll(event.follows.map { it.pubKeyHex })
                followsReceived++
                GlobalScope.launch {
                    val x = followsReceived
                    delay(2_000)
                    if (x == followsReceived) {
                        val filter =
                            """{"kinds":[${ContactListEvent.kind}],"authors":[${follows[1].joinToString(",") { "\"$it\"" }}]}"""
                        Client.addFilter(filter)
                        println("Phase three: Requesting user's follows' follows' follows")
                    }
                }
            } else if (event.pubKey.toHex() in follows[1]) {
                follows[2].addAll(event.follows.map { it.pubKeyHex })
                followsOfFollowsReceived ++
                GlobalScope.launch {
                    val x = followsOfFollowsReceived
                    delay(2_000)
                    if (x == followsOfFollowsReceived) {
                        val filter =
                            """{"kinds":[${MetadataEvent.kind}],"authors":[${(follows[2] + follows[1] + follows[0] + pubKey).joinToString(",") { "\"$it\"" }}]}"""
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
                    println("Following these ${follows[0].size}:")
                    follows[0].forEach {
                        println("f1: ${it} ${keyNames[it]}")
                    }
                    println("Following these further (circular follows removed) ${follows[1].size}:")
                    follows[1].forEach {
                        println("f2: ${it} ${keyNames[it]}")
                    }
                    println("Following these further (circular follows removed) ${follows[2].size}:")
                    follows[2].forEach {
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