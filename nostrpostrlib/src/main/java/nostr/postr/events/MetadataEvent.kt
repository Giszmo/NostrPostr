package nostr.postr.events

import com.google.gson.Gson
import nostr.postr.ContactMetaData

class MetadataEvent(
    id: ByteArray,
    pubKey: ByteArray,
    createdAt: Long,
    tags: List<List<String>>,
    content: String,
    sig: ByteArray
): Event(id, pubKey, createdAt, kind, tags, content, sig) {
    @Transient val contactMetaData: ContactMetaData

    init {
        try {
            contactMetaData = Gson().fromJson(content, ContactMetaData::class.java)
        } catch (e: Exception) {
            throw Error("can't parse $content", e)
        }
    }

    companion object {
        const val kind = 0
    }
}