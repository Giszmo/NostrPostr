package nostr.postr

import nostr.postr.events.Event
import nostr.postr.events.EventTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import java.util.*

internal class FilterTest {
    @Test
    fun filterKind() {
        val f = Filter(kinds = listOf(0, 4, 5))
        val events = events().filter { f.match(it) }
        assertEquals(135, events.count())
    }

    @Test
    fun filterMetaDate() {
        val f = Filter(
            kinds = listOf(0),
            since = Date(1654299430),
            until = Date(1654299430)
        )
        val events = events().filter { f.match(it) }
        assertEquals(1, events.count())
    }

    @Test
    fun referencesEvents() {
        val f = Filter(
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
        val filter = Filter.fromJson(json)
        assertEquals(1, filter.kinds!![0])
        assertEquals("bar, baz", filter.tags!!["foo"]!!.joinToString())
        assertEquals("lar, laz", filter.tags!!["loo"]!!.joinToString())
    }

    @Test
    fun fromJsonEmpty() {
        val json = "{}"
        Filter.fromJson(json).run {
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
        val filter = Filter(
            kinds = listOf(1),
            tags = mapOf(
                "foo" to listOf("bar", "baz"),
                "loo" to listOf("lar", "laz"),
                "e" to listOf(
                    "f6908a8b6323650dd34498efb191d951a5c7ef05d5a016eea4a27652b417ddbd",
                    "ec25b9c7ff8fa8ccdc7d2e3bfa06df82448a88c40212c6d19bce4a6f747b736b"
                )
            ),
            since = Date(1654299430)
        )
        assertEquals(json, filter.toJson())
    }

    @Test
    fun toJsonEmpty() {
        val json = "{}"
        val filter = Filter()
        assertEquals(json, filter.toJson())
    }

    companion object {
        private fun events() =
            EventTest::class.java.getResource("/event_kind_all.txt")!!
                .readText().split("\n").map(Event.Companion::fromJson)
    }
}