package nostr.postr

import com.github.mgunlogson.cuckoofilter4j.CuckooFilter
import com.google.common.hash.Funnels
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import nostr.postr.events.Event
import java.io.Serializable
import java.nio.charset.Charset
import java.util.*

interface Filter {
    fun match(event: Event): Boolean
}

object NoMatchFilter: Filter {
    fun toJson() = """{"ids":[]}"""

    override fun toString() = "NoMatchFilter"

    override fun match(event: Event) = false
}

object AllMatchFilter: Filter {
    fun toJson() = "{}"

    override fun toString() = "AllMatchFilter"

    override fun match(event: Event) = true
}

/**
 * Filters define what clients request from relays. They are defined in nip 1 and 12.
 */
class JsonFilter(
    val ids: List<String>? = null,
    val authors: List<String>? = null,
    val kinds: List<Int>? = null,
    val tags: Map<String, List<String>>? = null,
    val since: Long? = null,
    val until: Long? = null,
    val limit: Int? = null
) : Filter, Serializable {
    init {
        // Don't accept filters that are obviously not matching any valid Events
        check(
            !((ids?.isEmpty() == true ||
                    authors?.isEmpty() == true ||
                    kinds?.isEmpty() == true ||
                    tags?.isEmpty() == true ||
                    (since ?: Long.MIN_VALUE) > (until ?: Long.MAX_VALUE)))
        )
    }

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
            entries.forEach { kv ->
                jsonObject.add("#${kv.key}", JsonArray().apply { kv.value.forEach { add(it) } })
            }
        }
        since?.run {
            jsonObject.addProperty("since", since)
        }
        until?.run {
            jsonObject.addProperty("until", until)
        }
        limit?.run {
            jsonObject.addProperty("limit", limit)
        }
        return gson.toJson(jsonObject)
    }

    override fun match(event: Event): Boolean {
        if (ids?.any { event.id.toHex() == it } == false) return false
        if (kinds?.any { event.kind == it } == false) return false
        if (authors?.any { event.pubKey.toHex() == it } == false) return false
        tags?.forEach { tag ->
            if (!event.tags.any { it.first() == tag.key && it[1] in tag.value }) return false
        }
        if (event.createdAt !in (since ?: Long.MIN_VALUE)..(until ?: Long.MAX_VALUE))
            return false
        return true
    }

    /**
     * For keeping the Filter in memory, a relay can save a lot of space using ProbabilisticFilters
     * or for filters that match all content, to use one object.
     *
     * This space optimization ignores the `limit` parameter as it is not assumed to be applied on
     * stored data. Convert to space optimized only after querying the DB!
     */
    fun spaceOptimized(): Filter {
        if (ids == null &&
            authors == null &&
            kinds == null &&
            tags == null &&
            since == null &&
            until == null) {
            return AllMatchFilter
        }
        if(ids?.isEmpty() == true ||
            authors?.isEmpty() == true ||
            kinds?.isEmpty() == true ||
            tags?.isEmpty() == true ||
            (since ?: Long.MIN_VALUE) > (until ?: Long.MAX_VALUE)
        ) {
            return NoMatchFilter
        }
        if ((ids?.size ?: 0) + (authors?.size ?: 0) + (tags?.size ?: 0) < 10) {
            // if the filter has no compression potential, use it as is.
            return this
        }
        return ProbabilisticFilter.fromFilter(this)
    }

    override fun toString(): String = "JsonFilter${toJson()}"

    companion object {
        val gson: Gson = GsonBuilder().create()

        fun fromJson(json: String): JsonFilter {
            val jsonFilter = gson.fromJson(json, JsonObject::class.java)
            return fromJson(jsonFilter)
        }

        val declaredFields = JsonFilter::class.java.declaredFields.map { it.name }
        fun fromJson(json: JsonObject): JsonFilter {
            // sanity check
            if (json.keySet().any { !(it.startsWith("#") || it in declaredFields) }) {
                println("Filter $json contains unknown parameters.")
            }
            return JsonFilter(
                ids = if (json.has("ids")) json.getAsJsonArray("ids").map { it.asString } else null,
                authors = if (json.has("authors")) json.getAsJsonArray("authors")
                    .map { it.asString } else null,
                kinds = if (json.has("kinds")) json.getAsJsonArray("kinds")
                    .map { it.asInt } else null,
                tags = json
                    .entrySet()
                    .filter { it.key.startsWith("#") }
                    .associate {
                        it.key.substring(1) to it.value.asJsonArray.map { it.asString }
                    }
                    .ifEmpty { null },
                since = if (json.has("since")) json.get("since").asLong else null,
                until = if (json.has("until")) json.get("until").asLong else null,
                limit = if (json.has("limit")) json.get("limit").asInt else null
            )
        }
    }
}

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
    val since: Long? = null,
    val until: Long? = null,
    val limit: Int? = null,
    falsePositiveRate: Double = 1.0 / 10_000_000
) : Filter, Serializable {
    val ids: Boolean
    val authors: Boolean
    val tags: Boolean
    private val cuckooFilter: CuckooFilter<String>

    init {
        val stringCount = (ids?.size ?: 0) +
                (authors?.size ?: 0) +
                (tags?.flatMap { it.value }?.size ?: 0)
        cuckooFilter = CuckooFilter
            .Builder<String>(
                Funnels.stringFunnel(Charset.defaultCharset()),
                Integer.max(stringCount, 10)
            )
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

    override fun match(event: Event): Boolean {
        if (ids && !cuckooFilter.mightContain("e/${event.id.toHex()}")) return false
        if (kinds?.any { event.kind == it } == false) return false
        if (authors && !cuckooFilter.mightContain("p/${event.pubKey.toHex()}")) return false
        if (tags && event.tags.all { !cuckooFilter.mightContain("#${it.first()}/${it[1]}") }) return false
        if (event.createdAt !in (since ?: Long.MIN_VALUE)..(until ?: Long.MAX_VALUE))
            return false
        return true
    }

    override fun toString(): String {
        return "ProbabilisticFilter( " +
                (kinds?.let { "kinds: [${it.joinToString()}] " } ?: "") +
                (if (ids) "ids " else "") +
                (if (authors) "authors " else "") +
                (if (tags) "tags " else "") +
                (since?.let { "after: ${Date(it * 1000)} " } ?: "") +
                (until?.let { "until: ${Date(it * 1000)} " } ?: "") +
                ")"
    }

    companion object {
        val gson = GsonBuilder().create()

        fun fromJson(json: String): ProbabilisticFilter {
            val jsonFilter = gson.fromJson(json, JsonObject::class.java)
            return fromJson(jsonFilter)
        }

        fun fromJson(json: JsonObject): ProbabilisticFilter {
            // sanity check
            if (json.keySet().any { !(it.startsWith("#") || it in JsonFilter.declaredFields) }) {
                println("Filter $json contains unknown parameters.")
            }
            return ProbabilisticFilter(
                ids = if (json.has("ids")) json.getAsJsonArray("ids").map { it.asString } else null,
                authors = if (json.has("authors")) json.getAsJsonArray("authors")
                    .map { it.asString } else null,
                kinds = if (json.has("kinds")) json.getAsJsonArray("kinds")
                    .map { it.asInt } else null,
                tags = json
                    .entrySet()
                    .filter { it.key.startsWith("#") }
                    .associate {
                        it.key.substring(1) to it.value.asJsonArray.map { it.asString }
                    }
                    .ifEmpty { null },
                since = if (json.has("since")) json.get("since").asLong else null,
                until = if (json.has("until")) json.get("until").asLong else null,
                limit = if (json.has("limit")) json.get("limit").asInt else null
            )
        }

        fun fromFilter(filter: JsonFilter) = ProbabilisticFilter(
            ids = filter.ids,
            authors = filter.authors,
            kinds = filter.kinds,
            tags = filter.tags,
            since = filter.since,
            until = filter.until,
            limit = filter.limit
        )
    }
}
