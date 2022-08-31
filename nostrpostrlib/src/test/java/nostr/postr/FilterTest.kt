package nostr.postr

import nostr.postr.events.Event
import nostr.postr.events.EventTest
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

internal class FilterTest {
    @Test
    fun filterKind() {
        val f = JsonFilter(kinds = listOf(0, 4, 5))
        val events = events().filter { f.match(it) }
        assertEquals(135, events.count())
    }

    @Test
    fun filterMetaDate() {
        val f = JsonFilter(
            kinds = listOf(0),
            since = 1654299430,
            until = 1654299430
        )
        val events = events().filter { f.match(it) }
        assertEquals(1, events.count())
    }

    @Test
    fun referencesEvents() {
        val f = JsonFilter(
            tags = mapOf(
                "e" to listOf(
                    "f6908a8b6323650dd34498efb191d951a5c7ef05d5a016eea4a27652b417ddbd",
                    "ec25b9c7ff8fa8ccdc7d2e3bfa06df82448a88c40212c6d19bce4a6f747b736b"
                )
            )
        )
        val events = events().filter { f.match(it) }
        assertEquals(6, events.count())
    }

    @Test
    fun fromJson() {
        val json = """{"kinds":[1],"#foo":["bar","baz"],"#loo":["lar","laz"]}"""
        val filter = JsonFilter.fromJson(json)
        assertEquals(1, filter.kinds!![0])
        assertEquals("bar, baz", filter.tags!!["foo"]!!.joinToString())
        assertEquals("lar, laz", filter.tags!!["loo"]!!.joinToString())
    }

    @Test
    fun fromJsonEmpty() {
        val json = "{}"
        JsonFilter.fromJson(json).run {
            assertNull(kinds)
            assertNull(authors)
            assertNull(tags)
            assertNull(since)
            assertNull(until)
            assertNull(limit)
        }
    }

    @Test
    fun toJson() {
        val json =
            """{"kinds":[1],"#foo":["bar","baz"],"#loo":["lar","laz"],"#e":["f6908a8b6323650dd34498efb191d951a5c7ef05d5a016eea4a27652b417ddbd","ec25b9c7ff8fa8ccdc7d2e3bfa06df82448a88c40212c6d19bce4a6f747b736b"],"since":1654299430}"""
        val filter = JsonFilter(
            kinds = listOf(1),
            tags = mapOf(
                "foo" to listOf("bar", "baz"),
                "loo" to listOf("lar", "laz"),
                "e" to listOf(
                    "f6908a8b6323650dd34498efb191d951a5c7ef05d5a016eea4a27652b417ddbd",
                    "ec25b9c7ff8fa8ccdc7d2e3bfa06df82448a88c40212c6d19bce4a6f747b736b"
                )
            ),
            since = 1654299430
        )
        assertEquals(json, filter.toJson())
    }

    @Test
    fun toJsonEmpty() {
        val json = "{}"
        val filter = JsonFilter()
        assertEquals(json, filter.toJson())
    }

    @Test
    fun spaceOptimizedProbabilisticFilter() {
        val ids = (1..10).map { Persona().pubKey.toHex() }
        val authors = (1..10).map { Persona().pubKey.toHex() }
        val tags = mapOf(
            "e" to (1..2).map { Persona().pubKey.toHex() },
            "p" to (1..2).map { Persona().pubKey.toHex() },
            "a" to (1..2).map { Persona().pubKey.toHex() },
            "b" to (1..2).map { Persona().pubKey.toHex() },
            "c" to (1..2).map { Persona().pubKey.toHex() }
        )
        val jsonFilters = listOf(
            JsonFilter(ids = ids),
            JsonFilter(authors = authors),
            JsonFilter(authors = authors, kinds = listOf(1)),
            JsonFilter(tags = tags),
            JsonFilter(ids = ids, authors = authors, tags = tags)
        )
        jsonFilters.forEach {
            val optimized = it.spaceOptimized()
            Assertions.assertTrue(optimized is ProbabilisticFilter) { "Should have turned into ProbabilisticFilter: ${it.toJson()}" }
        }
    }

    @Test
    fun spaceOptimizedNoMatchFilter() {
        val tags = mapOf(
            "e" to emptyList(),
            "f" to listOf("A")
        )
        val jsonFilters = listOf(
            JsonFilter(ids = emptyList()),
            JsonFilter(authors = emptyList()),
            JsonFilter(tags = tags),
            JsonFilter(since = 5000, until = 4000),
            JsonFilter(
                ids = emptyList(),
                authors = emptyList(),
                tags = tags,
                since = 5000,
                until = 4000
            )
        )
        jsonFilters.forEach {
            val optimized = it.spaceOptimized()
            Assertions.assertTrue(optimized is NoMatchFilter) { "Should have turned into NoMatchFilter: ${it.toJson()}" }
        }
    }

    @Test
    fun spaceOptimizedAllMatchFilter() {
        val filter = JsonFilter()
        val optimized = filter.spaceOptimized()
        Assertions.assertTrue(optimized is AllMatchFilter) {
            "Should have turned into AllMatchFilter: ${filter.toJson()}"
        }
    }

    @Test
    fun spaceOptimizedJsonFilter() {
        val ids = (1..9).map { Persona().pubKey.toHex() }
        val authors = (1..9).map { Persona().pubKey.toHex() }
        val tags = mapOf(
            "e" to (1..2).map { Persona().pubKey.toHex() },
            "p" to (1..2).map { Persona().pubKey.toHex() },
            "a" to (1..2).map { Persona().pubKey.toHex() },
            "b" to (1..2).map { Persona().pubKey.toHex() }
        )
        val jsonFilters = listOf(
            JsonFilter(ids = ids),
            JsonFilter(authors = authors),
            JsonFilter(tags = tags),
            JsonFilter(since = 5, until = 35, limit = 0, kinds = listOf(1, 2, 3))
        )
        jsonFilters.forEach {
            val optimized = it.spaceOptimized()
            Assertions.assertTrue(optimized is JsonFilter) { "Should have turned into JsonFilter: ${it.toJson()}" }
        }
    }

    companion object {
        private fun events() =
            EventTest::class.java.getResource("/event_kind_all.txt")!!
                .readText().split("\n").map(Event.Companion::fromJson)
    }
}