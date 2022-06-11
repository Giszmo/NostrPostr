package nostr.postr

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

/**
 * Contains follows and used relays
 */
class ContactListEvent(
    id: ByteArray,
    pubkey: ByteArray,
    createdAt: Long,
    tags: List<List<String>>,
    content: String,
    sig: ByteArray
): Event(id, pubkey, createdAt, kind, tags, content, sig) {
    @Transient val follows: List<Contact>
    @Transient val relayUse: Map<String, ReadWrite>

    init {
        try {
            follows = tags.map {
                check(it[0] == "p")
                check(it.size > 1)
                check(it.size < 4)
                Contact(it[1], it.getOrNull(2))
            }
        } catch (e: Exception) {
            throw Error("can't parse tags as follows: $tags", e)
        }
        try {
            relayUse =  Gson().fromJson(content, object: TypeToken<Map<String, ReadWrite>>() {}.type)
        } catch (e: Exception) {
            throw Error("can't parse content as relays: $content", e)
        }
    }

    companion object {
        const val kind = 3
    }
    data class ReadWrite(val read: Boolean, val write: Boolean)
}