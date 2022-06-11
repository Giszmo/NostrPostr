package nostr.postr

class EncryptedDmEvent(
    id: ByteArray,
    pubkey: ByteArray,
    createdAt: Long,
    tags: List<List<String>>,
    content: String,
    sig: ByteArray
): Event(id, pubkey, createdAt, kind, tags, content, sig) {
    @Transient val recipient: String
    @Transient val replyToEvent: String?

    init {
        check(tags.size in 1..2)
        recipient = tags.first { it.firstOrNull() == "p" }.run { this[1] }
        replyToEvent = tags.firstOrNull { it.firstOrNull() == "e" }.run { this?.getOrNull(1) }
    }

    companion object {
        const val kind = 4
    }
}