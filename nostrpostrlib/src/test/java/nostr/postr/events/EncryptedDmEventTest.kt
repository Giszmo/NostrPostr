package nostr.postr.events

import nostr.postr.Persona
import nostr.postr.Utils
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.spongycastle.util.encoders.Hex

internal class EncryptedDmEventTest {
    @Test
    fun roundTrip() {
        val msg = "Hello World!"
        val alice = Persona(Hex.decode("aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa")).apply { petName = "Alice" }
        val bob = Persona(Hex.decode("bbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb")).apply { petName = "Bob" }
        val event = EncryptedDmEvent.create(bob.publicKey!!, null, msg, alice.privateKey!!)
        val msgDecrypted = Utils.decrypt(event.content, bob.privateKey!!, alice.publicKey!!)
        assertEquals(msg, msgDecrypted)
    }
}
