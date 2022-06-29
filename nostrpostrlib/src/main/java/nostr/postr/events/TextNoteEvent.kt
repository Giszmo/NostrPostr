package nostr.postr.events

import nostr.postr.Utils
import java.util.*

class TextNoteEvent(
    id: ByteArray,
    pubKey: ByteArray,
    createdAt: Long,
    tags: List<List<String>>,
    content: String,
    sig: ByteArray
): Event(id, pubKey, createdAt, kind, tags, content, sig) {
    @Transient val replyTos: List<String>
    @Transient val mentions: List<String>

    init {
        replyTos = tags.filter { it.firstOrNull() == "e" }.mapNotNull { it.getOrNull(1) }
        mentions = tags.filter { it.firstOrNull() == "p" }.mapNotNull { it.getOrNull(1) }
    }

    companion object {
        const val kind = 1

        fun create(msg: String, replyTos: List<String>?, mentions: List<String>?, privateKey: ByteArray, createdAt: Long = Date().time / 1000): TextNoteEvent {
            val pubKey = Utils.pubkeyCreate(privateKey)
            val tags = mutableListOf<List<String>>()
            replyTos?.forEach {
                tags.add(listOf("e", it))
            }
            mentions?.forEach {
                tags.add(listOf("p", it))
            }
            val id = generateId(pubKey, createdAt, kind, tags, msg)
            val sig = Utils.sign(id, privateKey)
            return TextNoteEvent(id, pubKey, createdAt, tags, msg, sig)
        }
    }
}