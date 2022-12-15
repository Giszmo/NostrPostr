package nostr.postr

import org.junit.jupiter.api.Assertions.*

import org.junit.jupiter.api.Test
import org.spongycastle.util.encoders.Hex

internal class UtilsTest {
    val alice = Persona(Hex.decode("a".repeat(64)))
    val bob = Persona(Hex.decode("b".repeat(64)))

    @Test
    fun privkeyCreate() {
        val privKey = Utils.privkeyCreate()
        assertEquals(32, privKey.size)
    }

    @Test
    fun pubkeyCreate() {
        val publicKey = Utils.pubkeyCreate(alice.privKey!!)
        assertEquals("6a04ab98d9e4774ad806e302dddeb63bea16b5cb5f223ee77478e861bb583eb3", publicKey.toHex())
    }

    @Test
    fun encryptDecrypt() {
        val msg = "Hello World!"
        val msgEncrypted = Utils.encrypt(msg, alice.privKey!!, bob.pubKey)
        val msgDecrypted = Utils.decrypt(msgEncrypted, bob.privKey!!, alice.pubKey)
        assertEquals(msg, msgDecrypted)
    }

    @Test
    fun getSharedSecret() {
        val sharedSecretAliceToBob = Utils.getSharedSecret(alice.privKey!!, bob.pubKey)
        val sharedSecretBobToAlice = Utils.getSharedSecret(bob.privKey!!, alice.pubKey)
        val sharedSecretExpected = Hex.decode("1d3e7279da3f845c4246087cdd3dd42bea3dea7245ceaf75609d8eb0a4e89c4e")
        assertArrayEquals(sharedSecretExpected, sharedSecretAliceToBob)
        assertArrayEquals(sharedSecretExpected, sharedSecretBobToAlice)
    }

    @Test
    fun convertPubkeyHexToNpub(){
        val pubkeyHex = "e4c47aedea8ea54255f5ba07a77053b24553e9b975435e56da343da19aec7881"
        val npubRepresentation = Hex.decode(pubkeyHex).toNpub()
        val correctRep = "npub1unz84m0236j5y404hgr6wuznkfz486dew4p4u4k6xs76rxhv0zqsq9q6t8"

        val jackPubkeyHex = "82341f882b6eabcd2ba7f1ef90aad961cf074af15b9ef44a09f9d2a8fbfbe6a2"
        val derivedJackNpub = Hex.decode(jackPubkeyHex).toNpub()
        val correctJackNpub = "npub1sg6plzptd64u62a878hep2kev88swjh3tw00gjsfl8f237lmu63q0uf63m"

        assertEquals(correctRep, npubRepresentation)
        assertEquals(correctJackNpub, derivedJackNpub)

    }
}