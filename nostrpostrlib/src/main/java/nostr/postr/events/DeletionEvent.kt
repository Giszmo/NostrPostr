package nostr.postr.events

class DeletionEvent(
    id: ByteArray,
    pubKey: ByteArray,
    createdAt: Long,
    tags: List<List<String>>,
    content: String,
    sig: ByteArray?
): Event(id, pubKey, createdAt, kind, tags, content, sig) {
    @Transient val deleteEvents: List<String>

    init {
        check(tags.isNotEmpty())
        check(tags.all { it[0] == "e" })
        deleteEvents = tags.map { it[1] }
    }

    companion object {
        const val kind = 5
    }
}