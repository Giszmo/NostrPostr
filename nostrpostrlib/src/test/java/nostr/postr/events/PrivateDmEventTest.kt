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
        val alice = Persona(Hex.decode("aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa"))
        val bob = Persona(Hex.decode("bbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb"))
        val sharedSecret = Utils.getSharedSecret(alice.privateKey!!, bob.publicKey)
        val sharedAccountPrivKey = PrivateDmEvent.getSharedAccountPrivKey(sharedSecret, 60000)
        val sharedAccountPubKey = Utils.pubkeyCreate(sharedAccountPrivKey)

        val replyTo = "70e18ff49f86c96e2c3a99e048d27437935d7c03080aeb7dfceef818995b904a"
        val event = PrivateDmEvent.create(bob.publicKey, replyTo, msg, alice.privateKey!!)
        val contentDecrypt = Utils.decrypt(event.content, bob.privateKey!!, alice.publicKey)
        println("""Alice pubkey is ${alice.publicKey.toHex()}
            |Bob pubkey is ${bob.publicKey.toHex()}
            |Shared secret is ${sharedSecret.toHex()}
            |Shared account privkey is ${sharedAccountPrivKey.toHex()}
            |Shared account pubkey is ${sharedAccountPubKey.toHex()}
            |The Event as seen by relays is
            |${event.toJson()}
            |
            |It sends "$msg" from Alice to Bob referencing event $replyTo.
            |
            |The content decrypted using the Alice-Bob shared secret as in kind-4 is
            |$contentDecrypt
        """.trimMargin())
        val msgDecrypted = event.plainContent(alice.publicKey, bob.privateKey!!)!!.content
        Assertions.assertEquals(msg, msgDecrypted)
    }
}