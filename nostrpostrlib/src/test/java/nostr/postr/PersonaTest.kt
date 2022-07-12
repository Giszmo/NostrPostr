package nostr.postr

import nostr.postr.events.TextNoteEvent
import org.junit.jupiter.api.Test

internal class PersonaTest {
    @Test
    fun sign() {
        val persona = Persona()
        val event = TextNoteEvent.create("Hello World!", null, null, persona.privKey!!, 50050505)
        event.checkSignature()
    }
}