package nostr.postr.events

import nostr.postr.Persona
import nostr.postr.Utils
import nostr.postr.toHex
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.spongycastle.util.encoders.Hex

internal class PrivateDmEventTest {
    @Test
    fun roundTrip() {
        val msg = "Hello World!"
        val alice = Persona(Hex.decode("a".repeat(64)))
        val bob = Persona(Hex.decode("b".repeat(64)))
        val carol = Persona(Hex.decode("c".repeat(64)))
        val sharedSecret = Utils.getSharedSecret(alice.privateKey!!, bob.publicKey)

        val event = PrivateDmEvent.create(bob.publicKey, msg, alice.privateKey!!, advertisedRecipientPubKey = carol.publicKey)
        val contentDecrypt = event.plainContent(alice.publicKey, bob.privateKey!!)
        val contentDecryptFail = event.plainContent(alice.publicKey, carol.privateKey!!)
        println("""Alice pubkey is ${alice.publicKey.toHex()}
            |Bob pubkey is ${bob.publicKey.toHex()}
            |Carol pubkey is ${carol.publicKey.toHex()}
            |Shared secret is ${sharedSecret.toHex()}
            |The Event as seen by relays is
            |${event.toJson()}
            |
            |It sends "$msg" from Alice to Bob.
            |
            |The content decrypted using the Alice-Bob shared secret as in kind-4 is
            |$contentDecrypt
            |
            |The content decrypted using the Alice-Carol shared secret as in kind-4 is
            |$contentDecryptFail
        """.trimMargin())
        Assertions.assertEquals(msg, contentDecrypt)
    }
}