package nostr.postr.events

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
    }
}