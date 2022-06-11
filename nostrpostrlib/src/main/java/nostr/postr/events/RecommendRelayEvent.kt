package nostr.postr

import java.net.URI

class RecommendRelayEvent(
    id: ByteArray,
    pubkey: ByteArray,
    createdAt: Long,
    tags: List<List<String>>,
    content: String,
    sig: ByteArray
): Event(id, pubkey, createdAt, kind, tags, content, sig) {
    @Transient val relay: String = URI.create(content).toString()

    companion object {
        const val kind = 2
    }
}