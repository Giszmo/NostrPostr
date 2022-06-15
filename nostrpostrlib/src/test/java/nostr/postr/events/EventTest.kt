package nostr.postr.events

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import org.spongycastle.util.encoders.Hex

class EventTest {
    @ParameterizedTest @MethodSource("provideAnyKindEventJson")
    fun fromToJson(eventJson: String) {
        check(eventJson.isNotEmpty())
        val eventJsonActual = Event.fromJson(eventJson).toJson()
        assertEquals(eventJson, eventJsonActual, "Actual events don't survive deserialization and re-serialization")
    }

    @ParameterizedTest @MethodSource("provideAnyKindEvent")
    fun generateId(event: Event) {
        val generatedId = event.generateId()
        assertEquals(String(Hex.encode(event.id)), String(Hex.encode(generatedId)))
    }

    @ParameterizedTest @MethodSource("provideAnyKindEventAll")
    fun checkSignature(event: Event) {
        event.checkSignature()
    }

    @ParameterizedTest @MethodSource("provideMetadataEvent")
    fun checkMetadataEvent(event: Event) {
        assertTrue(event is MetadataEvent)
    }

    @ParameterizedTest @MethodSource("provideTextNoteEvent")
    fun checkTextNoteEvent(event: Event) {
        assertTrue(event is TextNoteEvent)
    }

    @ParameterizedTest @MethodSource("provideRecommendRelayEvent")
    fun checkRecommendRelayEvent(event: Event) {
        assertTrue(event is RecommendRelayEvent)
    }

    @ParameterizedTest @MethodSource("provideContactListEvent")
    fun checkContactListEvent(event: Event) {
        assertTrue(event is ContactListEvent)
    }

    @ParameterizedTest @MethodSource("provideEncryptedDmEvent")
    fun checkEncryptedDmEvent(event: Event) {
        assertTrue(event is EncryptedDmEvent)
    }

    @ParameterizedTest @MethodSource("provideDeletionEvent")
    fun checkDeletionEvent(event: Event) {
        // TODO: provide test vectors in event_kind_5.txt
        assertTrue(event is DeletionEvent)
    }

    @ParameterizedTest @MethodSource("provideAnyKindEvent2")
    fun checkMoreEvents(event: Event) {
        when (event) {
            is MetadataEvent, is TextNoteEvent, is RecommendRelayEvent, is ContactListEvent, is EncryptedDmEvent -> Unit
            is DeletionEvent -> println(event.toJson())
            else -> println(event.toJson())
        }
    }

    companion object {
        private val eventsJson: Map<String, List<String>> = listOf("0", "1", "2", "3", "4", "5",
            "all", "all2", "failLong", "failShort").associateWith {
            EventTest::class.java.getResource("/event_kind_$it.txt")!!
                .readText()
                .split("\n")
        }

        private val events: Map<String, List<Event?>> by lazy {
            eventsJson.map {
                it.key to try {
                    it.value.map(Event.Companion::fromJson)
                } catch (e: Exception) {
                    println(e)
                    listOf()
                }
            }.toMap()
        }

        @JvmStatic fun provideAnyKindEventJson() = eventsJson["all"]!!.stream()
        @JvmStatic fun provideAnyKindEvent() = events["all"]!!.stream()
        @JvmStatic fun provideMetadataEvent() = events[MetadataEvent.kind.toString()]!!.stream()
        @JvmStatic fun provideTextNoteEvent() = events[TextNoteEvent.kind.toString()]!!.stream()
        @JvmStatic fun provideRecommendRelayEvent() = events[RecommendRelayEvent.kind.toString()]!!.stream()
        @JvmStatic fun provideContactListEvent() = events[ContactListEvent.kind.toString()]!!.stream()
        @JvmStatic fun provideEncryptedDmEvent() = events[EncryptedDmEvent.kind.toString()]!!.stream()
        @JvmStatic fun provideDeletionEvent() = events[DeletionEvent.kind.toString()]!!.stream()

        @JvmStatic fun provideAnyKindEvent2() = (events["all2"]!! + events["failShort"]!! + events["failLong"]!!).stream()
        @JvmStatic fun provideAnyKindEventAll() = listOf("0", "1", "2", "3", "4", "5",
            "all", "all2").map { events[it]!! }.fold(listOf<Event>()) { acc, list -> acc + list as List<Event> }.stream()
    }
}