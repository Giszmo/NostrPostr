package nostr.postr

import com.github.mgunlogson.cuckoofilter4j.CuckooFilter
import com.google.common.hash.Funnels
import com.google.gson.GsonBuilder
import com.google.gson.JsonObject
import nostr.postr.events.Event
import java.io.Serializable
import java.lang.Integer.max
import java.nio.charset.Charset
import java.util.*

/**
 * Filters define what clients request from relays. They are defined in nip 1 and 12.
 *
 * This version is memory friendly at the cost of delivering false positives.
 *
 * The ids, authors and tags dimensions are replaced with a single Cuckoo Filter
 */
class ProbabilisticFilter(
    ids: List<String>? = null,
    authors: List<String>? = null,
    val kinds: List<Int>? = null,
    tags: Map<String, List<String>>? = null,
    val since: Date? = null,
    val until: Date? = null,
    val limit: Int? = null,
    falsePositiveRate: Double = 1.0 / 10_000_000
): Serializable {
    val ids: Boolean
    val authors: Boolean
    val tags: Boolean
    private val cuckooFilter: CuckooFilter<String>

    init {
        val stringCount = (ids?.size ?: 0) +
                (authors?.size ?: 0) +
                (tags?.flatMap { it.value }?.size ?: 0)
        cuckooFilter = CuckooFilter
            .Builder<String>(Funnels.stringFunnel(Charset.defaultCharset()), max(stringCount, 10))
            .withFalsePositiveRate(falsePositiveRate)
            .build()
        ids?.forEach {
            cuckooFilter.put("e/$it")
        }
        authors?.forEach {
            cuckooFilter.put("p/$it")
        }
        tags?.forEach {
            val k = it.key
            it.value.forEach {
                cuckooFilter.put("#$k/$it")
            }
        }
        this.ids = ids != null
        this.authors = authors != null
        this.tags = tags != null
    }

    fun match(event: Event): Boolean {
        if (ids && !cuckooFilter.mightContain("e/${event.id.toHex()}")) return false
        if (kinds?.any { event.kind == it } == false) return false
        if (authors && !cuckooFilter.mightContain("p/${event.pubKey.toHex()}")) return false
        if (tags && event.tags.all { !cuckooFilter.mightContain("#${it.first()}/${it[1]}") }) return false
        if (event.createdAt !in (since?.time ?: Long.MIN_VALUE)..(until?.time ?: Long.MAX_VALUE))
            return false
        return true
    }

    companion object {
        val gson = GsonBuilder().create()

        fun fromJson(json: String): ProbabilisticFilter {
            val jsonFilter = gson.fromJson(json, JsonObject::class.java)
            return fromJson(jsonFilter)
        }

        fun fromJson(json: JsonObject): ProbabilisticFilter {
            // sanity check
            check(json.keySet().all { it.startsWith("#") || it in Filter.declaredFields })
            return ProbabilisticFilter(
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
