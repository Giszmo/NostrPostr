package nostr.postr.events

import nostr.postr.Utils
import nostr.postr.toByteArray
import java.util.*
import kotlin.experimental.xor

/**
 * This is the PoC for a new encrypted DM fixing meta data leakage of kind-4 EncryptedDmEvent.
 *
 * The nip-4 shared secret is used to derive accounts for the conversation.
 *
 * To avoid time correlation of users' activity using other events, it also uses a counter for the
 * creation of derived accounts.
 *
 * Problems with the current implementation:
 * 1. Clients have to query many accounts
 * 2. The list of queried accounts still reveals to the relays who they are talking to.
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
    /**
     * Tries to decrypt content assuming the provided keys
     */
    fun plainContent(pubKey: ByteArray, privKey: ByteArray, counter: Int = 0): PlainContent? {
        val sharedSecret = Utils.getSharedSecret(privKey, pubKey)
        val sharedAccountPrivKey = getSharedAccountPrivKey(sharedSecret, counter)
        val sharedAccountPubKey = Utils.pubkeyCreate(sharedAccountPrivKey)
        if (!this.pubKey.contentEquals(sharedAccountPubKey)) {
            return null
        }
        val plainContentJson = Utils.decrypt(content, sharedSecret)
        return PlainContent.create(plainContentJson)
    }

    companion object {
        const val kind = 42

        internal fun getSharedAccountPrivKey(sharedSecret: ByteArray, counter: Int = 0): ByteArray =
            Utils.sha256(sharedSecret.copyOf().apply {
                val counterBytes = counter.toByteArray()
                (0..3).forEach {
                    this[28 + it] = this[28 + it] xor counterBytes[it]
                }
            })

        fun create(recipientPubKey: ByteArray, replyTo: String?, msg: String, privateKey: ByteArray, createdAt: Long = Date().time / 1000, counter: Int = 0): PrivateDmEvent {
            val sharedSecret = Utils.getSharedSecret(privateKey, recipientPubKey)
            val sharedAccountPrivKey = getSharedAccountPrivKey(sharedSecret, counter)
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