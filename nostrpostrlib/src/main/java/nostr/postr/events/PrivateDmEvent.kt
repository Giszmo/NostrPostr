package nostr.postr.events

import fr.acinq.secp256k1.Hex
import nostr.postr.Utils
import nostr.postr.toHex
import java.util.*

/**
 * This is the nip-18 encrypted DM fixing meta data leakage of nip-04 EncryptedDmEvent. It is
 * compatible with nip-4 in that nip-4 Events can be loaded as nip-18 Events, deprecating
 * EncryptedDmEvent.
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
    @Transient val recipientPubKey: ByteArray?
    /**
     * To be fully compatible with nip-04, we read e-tags that are in violation to nip-18.
     *
     * Nip-18 messages should refer to other events by inline references in the content like
     * `[](e/c06f795e1234a9a1aecc731d768d4f3ca73e80031734767067c82d67ce82e506).
     */
    @Transient val replyTo: String?

    init {
        recipientPubKey = tags.firstOrNull { it.firstOrNull() == "p" }?.run { Hex.decode(this[1]) }
        replyTo = tags.firstOrNull { it.firstOrNull() == "e" }?.getOrNull(1)
    }

    /**
     * Tries to decrypt content using the event's pubkey with the provided key
     */
    fun plainContent(privKey: ByteArray) = plainContent(privKey, pubKey)

    /**
     * Tries to decrypt content using the provided keys
     */
    fun plainContent(privKey: ByteArray, pubKey: ByteArray): String? {
        val sharedSecret = Utils.getSharedSecret(privKey, pubKey)
        return try {
            val retVal = Utils.decrypt(content, sharedSecret)
            // decrypt randomly "succeeds". With the nip18Advertisement prefix we kill two birds
            // with one stone:
            // 1. We now can reliably tell gibberish from successfully decrypted messages.
            // 2. We get counter parties to enable nip18 without having the message visible where
            //    markdown is supported.
            if (retVal.startsWith(nip18Advertisement)) {
                retVal.substring(16)
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }

    companion object {
        const val kind = 4

        const val nip18Advertisement = "[//]: # (nip18)\n"

        fun create(
            recipientPubKey: ByteArray,
            msg: String,
            privateKey: ByteArray,
            createdAt: Long = Date().time / 1000,
            publishedRecipientPubKey: ByteArray? = null,
            advertiseNip18: Boolean = true
        ): PrivateDmEvent {
            val content = Utils.encrypt(
                if (advertiseNip18) { nip18Advertisement } else { "" } + msg,
                privateKey,
                recipientPubKey)
            val pubKey = Utils.pubkeyCreate(privateKey)
            val tags = mutableListOf<List<String>>()
            publishedRecipientPubKey?.let {
                tags.add(listOf("p", publishedRecipientPubKey.toHex()))
            }
            val id = generateId(pubKey, createdAt, kind, tags, content)
            val sig = Utils.sign(id, privateKey)
            return PrivateDmEvent(id, pubKey, createdAt, tags, content, sig)
        }
    }
}
