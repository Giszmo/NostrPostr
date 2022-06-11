package nostr.postr

class TextNoteEvent(
    id: ByteArray,
    pubkey: ByteArray,
    createdAt: Long,
    tags: List<List<String>>,
    content: String,
    sig: ByteArray
): Event(id, pubkey, createdAt, kind, tags, content, sig) {
    // TODO: reply to ... tag handling.
    companion object {
        const val kind = 1
    }
}