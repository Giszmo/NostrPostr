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
    // TODO: reply to ... tag handling.
    companion object {
        const val kind = 1

        fun create(msg: String, replyTos: List<String>?, mentions: List<String>?, privateKey: ByteArray, createdAt: Long = Date().time / 1000): TextNoteEvent {
            val pubKey = Utils.pubkeyCreate(privateKey)
            val tags = listOf<List<String>>()
            val id = generateId(pubKey, createdAt, kind, tags, msg)
            val sig = Utils.sign(id, privateKey)
            return TextNoteEvent(id, pubKey, createdAt, tags, msg, sig)
        }
    }
}