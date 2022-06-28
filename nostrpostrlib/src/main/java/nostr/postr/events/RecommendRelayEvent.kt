package nostr.postr.events

import nostr.postr.Utils
import java.net.URI
import java.util.*

class RecommendRelayEvent(
    id: ByteArray,
    pubKey: ByteArray,
    createdAt: Long,
    tags: List<List<String>>,
    content: String,
    sig: ByteArray,
    lenient: Boolean = false
): Event(id, pubKey, createdAt, kind, tags, content, sig) {
    @Transient val relay: URI

    init {
        relay = if (lenient)
            URI.create(content.trim())
        else
            URI.create(content)
    }

    companion object {
        const val kind = 2

        fun create(relay: URI, privateKey: ByteArray, createdAt: Long = Date().time / 1000): RecommendRelayEvent {
            val content = relay.toString()
            val pubKey = Utils.pubkeyCreate(privateKey)
            val tags = listOf<List<String>>()
            val id = generateId(pubKey, createdAt, kind, tags, content)
            val sig = Utils.sign(id, privateKey)
            return RecommendRelayEvent(id, pubKey, createdAt, tags, content, sig)
        }
    }
}