package nostr.postr

import com.google.gson.GsonBuilder
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import nostr.postr.events.Event
import java.io.Serializable
import java.util.*

/**
 * Filters define what clients request from relays. They are defined in nip 1 and 12.
 */
class Filter(
    val ids: List<String>? = null,
    val authors: List<String>? = null,
    val kinds: List<Int>? = null,
    val tags: Map<String, List<String>>? = null,
    val since: Date? = null,
    val until: Date? = null,
    val limit: Int? = null
): Serializable {
    fun toJson(): String {
        val jsonObject = JsonObject()
        ids?.run {
            jsonObject.add("ids", JsonArray().apply { ids.forEach { add(it) } })
        }
        authors?.run {
            jsonObject.add("authors", JsonArray().apply { authors.forEach { add(it) } })
        }
        kinds?.run {
            jsonObject.add("kinds", JsonArray().apply { kinds.forEach { add(it) } })
        }
        tags?.run {
            tags.entries.forEach { kv ->
                jsonObject.add("#${kv.key}", JsonArray().apply { kv.value.forEach { add(it) } })
            }
        }
        since?.run {
            jsonObject.addProperty("since", since.time)
        }
        until?.run {
            jsonObject.addProperty("until", until.time)
        }
        limit?.run {
            jsonObject.addProperty("limit", limit)
        }
        return gson.toJson(jsonObject)
    }

    fun match(event: Event): Boolean {
        if (ids?.any { event.id.toHex() == it } == false) return false
        if (kinds?.any { event.kind == it } == false) return false
        if (authors?.any { event.pubKey.toHex() == it } == false) return false
        tags?.forEach { tag ->
            if (!event.tags.any { it.first() == tag.key && it[1] in tag.value }) return false
        }
        if (event.createdAt !in (since?.time ?: Long.MIN_VALUE)..(until?.time ?: Long.MAX_VALUE))
            return false
        return true
    }

    companion object {
        val gson = GsonBuilder().create()

        fun fromJson(json: String): Filter {
            val jsonFilter = gson.fromJson(json, JsonObject::class.java)
            return fromJson(jsonFilter)
        }
        val declaredFields = Filter::class.java.declaredFields.map { it.name }
        fun fromJson(json: JsonObject): Filter {
            // sanity check
            check(json.keySet().all { it.startsWith("#") || it in declaredFields })
            return Filter(
                ids = if (json.has("ids")) json.getAsJsonArray("ids").map { it.asString } else null,
                authors = if (json.has("authors")) json.getAsJsonArray("authors").map { it.asString } else null,
                kinds = if (json.has("kinds")) json.getAsJsonArray("kinds").map { it.asInt } else null,
                tags = json
                    .entrySet()
                    .filter { it.key.startsWith("#") }
                    .associate {
                        it.key.substring(1) to it.value.asJsonArray.map { it.asString }
                    }
                    .ifEmpty { null },
                since = if (json.has("since")) Date(json.get("since").asLong) else null,
                until = if (json.has("until")) Date(json.get("until").asLong) else null,
                limit = if (json.has("limit")) json.get("limit").asInt else null
            )
        }
    }
}
