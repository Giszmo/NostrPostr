package nostr.postr

import nostr.postr.events.Event
import nostr.postr.events.EventTest
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.io.ByteArrayOutputStream
import java.io.ObjectOutputStream
import java.io.Serializable

internal class ProbabilisticFilterTest {
    @Test
    fun filterKind() {
        val f = ProbabilisticFilter(kinds = listOf(0, 4, 5))
        val events = events().filter { f.match(it) }
        assertEquals(135, events.count())
    }

    @Test
    fun filterMetaDate() {
        val f = ProbabilisticFilter(
            kinds = listOf(0),
            since = 1654299430,
            until = 1654299430
        )
        val events = events().filter { f.match(it) }
        assertEquals(1, events.count())
    }

    @Test
    fun referencesEvents() {
        val f = ProbabilisticFilter(
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
        val filter = ProbabilisticFilter.fromJson(json)
        assertEquals(1, filter.kinds!![0])
        assertTrue(filter.tags)
    }

    @Test
    fun fromJsonEmpty() {
        val json = "{}"
        ProbabilisticFilter.fromJson(json).run {
            assertFalse(ids)
            assertNull(kinds)
            assertFalse(authors)
            assertFalse(tags)
            assertNull(since)
            assertNull(until)
            assertNull(limit)
        }
    }

    @Test
    fun sizeComparison() {
        val ids = (1..5000).map { Persona().pubKey.toHex() }
        val authors = (1..5000).map { Persona().pubKey.toHex() }
        val tags = mapOf(
            "#e" to (1..1000).map { Persona().pubKey.toHex() },
            "#p" to (1..1000).map { Persona().pubKey.toHex() },
            "#a" to (1..1000).map { Persona().pubKey.toHex() },
            "#b" to (1..1000).map { Persona().pubKey.toHex() },
            "#c" to (1..1000).map { Persona().pubKey.toHex() }
        )

        val filterPairs = listOf(
            Pair(JsonFilter(ids = ids), ProbabilisticFilter(ids = ids)),
            Pair(JsonFilter(authors = authors), ProbabilisticFilter(authors = authors)),
            Pair(JsonFilter(tags = tags), ProbabilisticFilter(tags = tags)),
            Pair(JsonFilter(tags = tags, authors = authors, ids = ids), ProbabilisticFilter(tags = tags, authors = authors, ids = ids)),
            Pair(JsonFilter(), ProbabilisticFilter())
        )

        filterPairs.forEach {
            val sizeFirst = getSize(it.first)
            val sizeSecond = getSize(it.second)
            println("${sizeFirst}B vs. ${sizeSecond}B (saving ${100 - 100 * sizeSecond / sizeFirst}%) for ${it.second}")
        }
    }

    fun getSize(ser: Serializable): Int {
        val baos = ByteArrayOutputStream()
        val oos = ObjectOutputStream(baos)
        oos.writeObject(ser)
        oos.close()
        return baos.size()
    }

    companion object {
        private fun events() =
            EventTest::class.java.getResource("/event_kind_all.txt")!!
                .readText().split("\n").map(Event.Companion::fromJson)
    }
}