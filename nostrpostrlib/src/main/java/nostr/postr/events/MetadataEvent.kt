package nostr.postr

import com.google.gson.Gson

class MetadataEvent(
    id: ByteArray,
    pubkey: ByteArray,
    createdAt: Long,
    tags: List<List<String>>,
    content: String,
    sig: ByteArray
): Event(id, pubkey, createdAt, kind, tags, content, sig) {
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