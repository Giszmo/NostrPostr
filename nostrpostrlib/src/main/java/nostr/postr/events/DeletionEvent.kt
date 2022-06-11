package nostr.postr

class DeletionEvent(
    id: ByteArray,
    pubkey: ByteArray,
    createdAt: Long,
    tags: List<List<String>>,
    content: String,
    sig: ByteArray
): Event(id, pubkey, createdAt, kind, tags, content, sig) {
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