package nostr.postr.events

import fr.acinq.secp256k1.Hex
import nostr.postr.Utils
import nostr.postr.toHex
import java.util.*

@Deprecated("Use nip-18 PrivateDmEvent instead.")
class EncryptedDmEvent(
    id: ByteArray,
    pubKey: ByteArray,
    createdAt: Long,
    tags: List<List<String>>,
    content: String,
    sig: ByteArray
): Event(id, pubKey, createdAt, kind, tags, content, sig) {
    @Transient val recipientPubKey: ByteArray
    @Transient val replyTo: String?

    init {
        check(tags.size in 1..2)
        recipientPubKey = tags.first { it.firstOrNull() == "p" }.run { Hex.decode(this[1]) }
        replyTo = tags.firstOrNull { it.firstOrNull() == "e" }.run { this?.getOrNull(1) }
    }

    companion object {
        const val kind = 4

        fun create(recipientPubKey: ByteArray, replyTo: String?, msg: String, privateKey: ByteArray, createdAt: Long = Date().time / 1000): EncryptedDmEvent {
            val content = Utils.encrypt(msg, privateKey, recipientPubKey)
            val pubKey = Utils.pubkeyCreate(privateKey)
            val tags = mutableListOf(listOf("p", recipientPubKey.toHex()))
            if (replyTo != null) {
                tags.add(listOf("e", replyTo))
            }
            val id = generateId(pubKey, createdAt, kind, tags, content)
            val sig = Utils.sign(id, privateKey)
            return EncryptedDmEvent(id, pubKey, createdAt, tags, content, sig)
        }
    }
}

