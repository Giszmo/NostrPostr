package nostr.postr

import com.google.gson.GsonBuilder
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import nostr.postr.events.Event
import java.util.*

/**
 * Filters define what clients request from relays. They are defined in nip 1 and 12.
 */
class Filter(
    val events: List<String>? = null,
    val authors: List<String>? = null,
    val kinds: List<Int>? = null,
    val tags: Map<String, List<String>>? = null,
    val since: Date? = null,
    val until: Date? = null,
    val limit: Int? = null
) {
    fun toJson(): String {
        val jsonObject = JsonObject()
        events?.run {
            jsonObject.add("ids", JsonArray().apply { events.forEach { add(it) } })
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
        if (events?.any { event.id.toHex() == it } == false) return false
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
            return Filter(
                events = if (jsonFilter.has("events")) jsonFilter.getAsJsonArray("events").map { it.asString } else null,
                authors = if (jsonFilter.has("authors")) jsonFilter.getAsJsonArray("authors").map { it.asString } else null,
                kinds = if (jsonFilter.has("kinds")) jsonFilter.getAsJsonArray("kinds").map { it.asInt } else null,
                tags = jsonFilter
                    .entrySet()
                    .filter { it.key.startsWith("#") }
                    .associate {
                        it.key.substring(1) to it.value.asJsonArray.map { it.asString }
                    }
                    .ifEmpty { null },
                since = if (jsonFilter.has("since")) Date(jsonFilter.get("since").asLong) else null,
                until = if (jsonFilter.has("until")) Date(jsonFilter.get("until").asLong) else null,
                limit = if (jsonFilter.has("limit")) jsonFilter.get("limit").asInt else null
            )
        }
    }
}
