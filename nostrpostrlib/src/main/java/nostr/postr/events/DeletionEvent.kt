package nostr.postr.events

import nostr.postr.Utils
import java.util.*

class DeletionEvent(
    id: ByteArray,
    pubKey: ByteArray,
    createdAt: Long,
    tags: List<List<String>>,
    content: String,
    sig: ByteArray
): Event(id, pubKey, createdAt, kind, tags, content, sig) {
    @Transient val deleteEvents: List<String>

    init {
        check(tags.isNotEmpty())
        check(tags.all { it[0] == "e" })
        deleteEvents = tags.map { it[1] }
    }

    companion object {
        const val kind = 5

        fun create(deleteEvents: List<String>, privateKey: ByteArray, createdAt: Long = Date().time / 1000): DeletionEvent {
            val content = ""
            val pubKey = Utils.pubkeyCreate(privateKey)
            val tags = deleteEvents.map { listOf("e", it) }
            val id = generateId(pubKey, createdAt, kind, tags, content)
            val sig = Utils.sign(id, privateKey)
            return DeletionEvent(id, pubKey, createdAt, tags, content, sig)
        }
    }
}