package nostr.postr.events

import fr.acinq.secp256k1.Hex
import nostr.postr.Utils
import nostr.postr.toHex
import java.util.*

/**
 * This is the PoC for nip-18 encrypted DM fixing meta data leakage of nip-04 EncryptedDmEvent.
 */
class PrivateDmEvent(
    id: ByteArray,
    pubKey: ByteArray,
    createdAt: Long,
    tags: List<List<String>>,
    content: String,
    sig: ByteArray
) : Event(id, pubKey, createdAt, kind, tags, content, sig) {
    /**
     * This may or may not be the actual recipient's pub key. The event is intended to look like a
     * nip-04 EncryptedDmEvent but may omit the recipient, too. This value can be queried and used
     * for initial messages.
     */
    @Transient
    val recipientPubKey: ByteArray?

    /**
     * Tries to decrypt content assuming the provided keys
     */
    fun plainContent(pubKey: ByteArray, privKey: ByteArray): String? {
        val sharedSecret = Utils.getSharedSecret(privKey, pubKey)
        return try {
            Utils.decrypt(content, sharedSecret)
        } catch (e: Exception) {
            null
        }
    }

    init {
        recipientPubKey = tags.firstOrNull { it.firstOrNull() == "p" }?.run { Hex.decode(this[1]) }
    }

    companion object {
        const val kind = 4

        fun create(
            recipientPubKey: ByteArray,
            msg: String,
            privateKey: ByteArray,
            createdAt: Long = Date().time / 1000,
            advertisedRecipientPubKey: ByteArray? = null
        ): PrivateDmEvent {
            val content = Utils.encrypt(msg, privateKey, recipientPubKey)
            val pubKey = Utils.pubkeyCreate(privateKey)
            val tags = mutableListOf<List<String>>()
            advertisedRecipientPubKey?.let {
                tags.add(listOf("p", advertisedRecipientPubKey.toHex()))
            }
            val id = generateId(pubKey, createdAt, kind, tags, content)
            val sig = Utils.sign(id, privateKey)
            return PrivateDmEvent(id, pubKey, createdAt, tags, content, sig)
        }
    }
}
