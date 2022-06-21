package nostr.postr.events

import java.net.URI

class RecommendRelayEvent(
    id: ByteArray,
    pubKey: ByteArray,
    createdAt: Long,
    tags: List<List<String>>,
    content: String,
    sig: ByteArray
): Event(id, pubKey, createdAt, kind, tags, content, sig) {
    @Transient val relay: URI = URI.create(content)

    companion object {
        const val kind = 2
    }
}