package nostr.postr.events

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import nostr.postr.Contact
import nostr.postr.Utils
import java.util.*

/**
 * Contains follows and used relays
 */
class ContactListEvent(
    id: ByteArray,
    pubKey: ByteArray,
    createdAt: Long,
    tags: List<List<String>>,
    content: String,
    sig: ByteArray
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
                gson.fromJson(content, object: TypeToken<Map<String, ReadWrite>>() {}.type)
            else
                null
        } catch (e: Exception) {
            null
        }
    }

    companion object {
        const val kind = 3

        fun create(follows: List<Contact>, relayUse: Map<String, ReadWrite>?, privateKey: ByteArray, createdAt: Long = Date().time / 1000): ContactListEvent {
            val content = if (relayUse != null)
                gson.toJson(relayUse)
            else
                ""
            val pubKey = Utils.pubkeyCreate(privateKey)
            val tags = follows.map {
                if (it.relayUri != null)
                    listOf("p", it.pubKeyHex, it.relayUri)
                else
                    listOf("p", it.pubKeyHex)
            }
            val id = generateId(pubKey, createdAt, kind, tags, content)
            val sig = Utils.sign(id, privateKey)
            return ContactListEvent(id, pubKey, createdAt, tags, content, sig)
        }
    }

    data class ReadWrite(val read: Boolean, val write: Boolean)
}