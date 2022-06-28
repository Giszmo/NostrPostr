package nostr.postr.events

import com.google.gson.Gson
import nostr.postr.ContactMetaData
import nostr.postr.Utils
import java.util.*

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
            contactMetaData = gson.fromJson(content, ContactMetaData::class.java)
        } catch (e: Exception) {
            throw Error("can't parse $content", e)
        }
    }

    companion object {
        const val kind = 0
        val gson = Gson()

        fun create(contactMetaData: ContactMetaData, privateKey: ByteArray, createdAt: Long = Date().time / 1000): MetadataEvent {
            val content = gson.toJson(contactMetaData)
            val pubKey = Utils.pubkeyCreate(privateKey)
            val tags = listOf<List<String>>()
            val id = generateId(pubKey, createdAt, kind, tags, content)
            val sig = Utils.sign(id, privateKey)
            return MetadataEvent(id, pubKey, createdAt, tags, content, sig)
        }
    }
}