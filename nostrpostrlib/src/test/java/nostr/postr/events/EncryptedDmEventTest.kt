package nostr.postr.events

import nostr.postr.Persona
import nostr.postr.Utils
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.spongycastle.util.encoders.Hex

internal class EncryptedDmEventTest {
    @Test
    fun roundTrip() {
        val msg = "Hello World!"
        val alice = Persona(Hex.decode("aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa"))
        val bob = Persona(Hex.decode("bbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb"))
        val event = EncryptedDmEvent.create(bob.pubKey, null, msg, alice.privKey!!)
        val msgDecrypted = Utils.decrypt(event.content, bob.privKey!!, alice.pubKey)
        assertEquals(msg, msgDecrypted)
    }
}

