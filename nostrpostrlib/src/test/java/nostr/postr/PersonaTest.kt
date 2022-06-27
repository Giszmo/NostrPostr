package nostr.postr

import nostr.postr.events.Event
import nostr.postr.events.Event.Companion.sign
import nostr.postr.events.TextNoteEvent
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.util.*

internal class PersonaTest {
    @Test
    fun sign() {
        val persona = Persona()
        val event = Event(ByteArray(0), persona.publicKey!!, 50050505, 1, listOf(), "Hello World!", ByteArray(0))
        event.sign(persona.privateKey!!)
        event.checkSignature()
    }
}