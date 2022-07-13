package nostr.postr.events

import nostr.postr.Persona
import nostr.postr.Utils
import nostr.postr.toHex
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertTimeout
import org.spongycastle.util.encoders.Hex
import java.time.Duration

internal class PrivateDmEventTest {
    val alice = Persona(Hex.decode("a".repeat(64)))
    val bob = Persona(Hex.decode("b".repeat(64)))
    val carol = Persona(Hex.decode("c".repeat(64)))
    val msg = "Hello World!"
    val event = PrivateDmEvent.create(
        bob.pubKey,
        msg,
        alice.privKey!!,
        publishedRecipientPubKey = carol.pubKey)

    @Test
    fun succeedToDecrypt() {
        assertEquals(msg, event.plainContent(bob.privKey!!))
        assertEquals(msg, event.plainContent(alice.privKey!!, bob.pubKey))
    }

    @Test
    fun example() {
        println("""privAlice:         ${alice.privKey!!.toHex()}
pubAlice:          ${alice.pubKey.toHex()}
privBob:           ${bob.privKey!!.toHex()}
pubBob:            ${bob.pubKey.toHex()}
privCarol:         ${carol.privKey!!.toHex()}
pubCarol:          ${carol.pubKey.toHex()}

sharedSecretAB:    ${Utils.getSharedSecret(alice.privKey!!, bob.pubKey).toHex()}
msg:               $msg
event:             ${event.toJson()}
contentBobNip04:   ${Utils.decrypt(event.content, alice.privKey!!, bob.pubKey)}
contentBobNip18:   ${event.plainContent(bob.privKey!!)}
contentCarol:      Error
""")
    }

    @Test
    fun failToDecryptPerformance() {
        val personas = (1..10_000).map { Persona() }
        assertTimeout(
            Duration.ofSeconds(1),
            "If performance drops below 10k/s, check what's going on. It was 15k/s.") {
            personas.forEach {
                assertNull(event.plainContent(it.privKey!!))
            }
        }
    }
}