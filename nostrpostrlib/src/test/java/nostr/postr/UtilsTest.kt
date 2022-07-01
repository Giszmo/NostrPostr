package nostr.postr

import org.junit.jupiter.api.Assertions.*

import org.junit.jupiter.api.Test
import org.spongycastle.util.encoders.Hex

internal class UtilsTest {
    val alice = Persona(Hex.decode("aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa"))
    val bob = Persona(Hex.decode("bbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb"))

    @Test
    fun privkeyCreate() {
        val privKey = Utils.privkeyCreate()
        assertEquals(32, privKey.size)
    }

    @Test
    fun pubkeyCreate() {
        val publicKey = Utils.pubkeyCreate(alice.privateKey!!)
        assertEquals("6a04ab98d9e4774ad806e302dddeb63bea16b5cb5f223ee77478e861bb583eb3", publicKey.toHex())
    }

    @Test
    fun encryptDecrypt() {
        val msg = "Hello World!"
        val msgEncrypted = Utils.encrypt(msg, alice.privateKey!!, bob.publicKey)
        val msgDecrypted = Utils.decrypt(msgEncrypted, bob.privateKey!!, alice.publicKey)
        assertEquals(msg, msgDecrypted)
    }

    @Test
    fun getSharedSecret() {
        val sharedSecretAliceToBob = Utils.getSharedSecret(alice.privateKey!!, bob.publicKey)
        val sharedSecretBobToAlice = Utils.getSharedSecret(bob.privateKey!!, alice.publicKey)
        assertEquals("1d3e7279da3f845c4246087cdd3dd42bea3dea7245ceaf75609d8eb0a4e89c4e", sharedSecretAliceToBob.toHex())
        assertEquals("1d3e7279da3f845c4246087cdd3dd42bea3dea7245ceaf75609d8eb0a4e89c4e", sharedSecretBobToAlice.toHex())
    }
}