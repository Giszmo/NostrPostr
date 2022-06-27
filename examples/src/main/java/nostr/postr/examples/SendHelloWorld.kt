package nostr.postr.examples

import nostr.postr.Client
import nostr.postr.Filter
import nostr.postr.Persona
import nostr.postr.events.Event
import nostr.postr.events.TextNoteEvent
import nostr.postr.toHex
import java.util.*
import org.spongycastle.util.encoders.Hex

class SendHelloWorld {
    companion object {
        private val persona = Persona(Hex.decode("ed677a60034a04bb282e1b4587e1ece5c5b81e2261d7aeea933c0ee07095df80"))

        private val listener = object: Client.Listener() {
            override fun onNewEvent(event: Event) {
                if (event.pubKey.toHex() == persona.publicKey!!.toHex() && event is TextNoteEvent) {
                    logDetail(event, event.content)
                    stop()
                } else {
                    logDetail(event, "Why do we get this event? ${event.id}")
                }
            }
        }

        @JvmStatic
        fun main(vararg args: String) {
            println("""Persona(privKey:${persona.privateKey!!.toHex()}, pubKey:${persona.publicKey!!.toHex()})""")
            Client.subscribe(listener)
            Client.connect(mutableListOf(Filter(
                kinds = listOf(TextNoteEvent.kind),
                authors = listOf(persona.publicKey!!.toHex()))))
            val event = TextNoteEvent(ByteArray(0), persona.publicKey!!, Date().time, listOf(), "Hello World!", ByteArray(0))
            val signedEvent = persona.sign(event)
            Client.send(signedEvent)
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