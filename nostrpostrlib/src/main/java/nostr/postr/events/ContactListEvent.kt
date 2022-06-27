package nostr.postr.events

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import nostr.postr.Contact

/**
 * Contains follows and used relays
 */
class ContactListEvent(
    id: ByteArray,
    pubKey: ByteArray,
    createdAt: Long,
    tags: List<List<String>>,
    content: String,
    sig: ByteArray?
): Event(id, pubKey, createdAt, kind, tags, content, sig) {
    @Transient val follows: List<Contact>
    @Transient val relayUse: Map<String, ReadWrite>?

    init {
        try {
            follows = tags.filter { it[0] == "p" }.map {
                Contact(it[1], it.getOrNull(2))
            }
        } catch (e: Exception) {
            throw Error("can't parse tags as follows: $tags", e)
        }
        relayUse = try {
            if (content.isNotEmpty())
                Gson().fromJson(content, object: TypeToken<Map<String, ReadWrite>>() {}.type)
            else
                null
        } catch (e: Exception) {
            null
        }
    }

    companion object {
        const val kind = 3
    }
    data class ReadWrite(val read: Boolean, val write: Boolean)
}