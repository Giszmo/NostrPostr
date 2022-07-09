package nostr.postr.events

import nostr.postr.Utils
import java.util.*

/**
 * This is the PoC for a new encrypted DM fixing meta data leakage of kind-4 EncryptedDmEvent.
 *
 * https://github.com/nostr-protocol/nostr/issues/69#issuecomment-1176661830
 */
class PrivateDmEvent(
    id: ByteArray,
    pubKey: ByteArray,
    createdAt: Long,
    tags: List<List<String>>,
    content: String,
    sig: ByteArray
): Event(id, pubKey, createdAt, kind, tags, content, sig) {

    init {
        check(tags.isEmpty())
    }

    /**
     * Tries to decrypt content assuming the provided keys
     */
    fun plainContent(pubKey: ByteArray, privKey: ByteArray): PlainContent? {
        val sharedSecret = Utils.getSharedSecret(privKey, pubKey)
        val sharedAccountPrivKey = Utils.sha256(sharedSecret)
        val sharedAccountPubKey = Utils.pubkeyCreate(sharedAccountPrivKey)
        if (!this.pubKey.contentEquals(sharedAccountPubKey)) {
            return null
        }
        val plainContentJson = Utils.decrypt(content, sharedSecret)
        return PlainContent.create(plainContentJson)
    }

    companion object {
        const val kind = 42

        fun create(recipientPubKey: ByteArray, replyTo: String?, msg: String, privateKey: ByteArray, createdAt: Long = Date().time / 1000): PrivateDmEvent {
            val sharedSecret = Utils.getSharedSecret(privateKey, recipientPubKey)
            val sharedAccountPrivKey = Utils.sha256(sharedSecret)
            val sharedAccountPubKey = Utils.pubkeyCreate(sharedAccountPrivKey)
            val plainContent = PlainContent.create(replyTo?.let { listOf(it) } ?: emptyList(), msg, privateKey)
            val content = Utils.encrypt(plainContent.toJson(), sharedSecret)
            val id = generateId(sharedAccountPubKey, createdAt, kind, emptyList(), content)
            val sig = Utils.sign(id, sharedAccountPrivKey)
            return PrivateDmEvent(id, sharedAccountPubKey, createdAt, emptyList(), content, sig)
        }
    }

    class PlainContent(val pubKey: ByteArray, val tags: List<List<String>>, val content: String, val sig: ByteArray) {
        fun toJson(): String = gson.toJson(this)

        companion object {
            fun create(replyTos: List<String>, content: String, privateKey: ByteArray): PlainContent {
                val pubKey = Utils.pubkeyCreate(privateKey)
                val tags = mutableListOf<List<String>>()
                replyTos.forEach {
                    tags.add(listOf("e", it))
                }
                val sig = Utils.sign(generateId(pubKey, 0, 0, tags, content), privateKey)
                return PlainContent(pubKey, tags, content, sig)
            }

            fun create(plainContentJson: String): PlainContent =
                gson.fromJson(plainContentJson, PlainContent::class.java)
        }
    }
}