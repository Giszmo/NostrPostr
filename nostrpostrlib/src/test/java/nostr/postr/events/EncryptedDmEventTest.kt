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
        val alice = Persona(Hex.decode("aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa"))
        val bob = Persona(Hex.decode("bbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb"))
        val event = EncryptedDmEvent.create(bob.publicKey, null, msg, alice.privateKey!!)
        val msgDecrypted = Utils.decrypt(event.content, bob.privateKey!!, alice.publicKey)
        assertEquals(msg, msgDecrypted)
    }
}

internal class PrivateDmEventTest {
    @Test fun roundTrip() {
        val msg = "Hello World!"
        val alice = Persona(Hex.decode("aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa"))
        val bob = Persona(Hex.decode("bbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb"))
        val event = PrivateDmEvent.create(bob.publicKey, null, msg, alice.privateKey!!)
        println(event.toJson())
        println(Utils.decrypt(event.content, bob.privateKey!!, alice.publicKey))
        val msgDecrypted = event.plainContent(alice.publicKey, bob.privateKey!!)!!.content
        assertEquals(msg, msgDecrypted)
    }
}
