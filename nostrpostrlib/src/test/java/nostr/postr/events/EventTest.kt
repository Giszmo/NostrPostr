package nostr.postr.events

import nostr.postr.*
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import org.spongycastle.util.encoders.Hex

class EventTest {
    @ParameterizedTest @MethodSource("provideAnyKindEventJson")
    fun fromToJson(eventJson: String) {
        check(eventJson.isNotEmpty())
        val event = Event.fromJson(eventJson)
        val eventJsonActual = event!!.toJson()
        assertEquals(eventJson, eventJsonActual, "Actual events don't survive deserialization and re-serialization")
    }

    @ParameterizedTest @MethodSource("provideAnyKindEvent")
    fun generateId(event: Event) {
        val generatedId = event.generateId()
        assertEquals(String(Hex.encode(event.id)), String(Hex.encode(generatedId)))
    }

    @ParameterizedTest @MethodSource("provideAnyKindEvent")
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

    companion object {
        private val eventsJson: Map<String, List<String>> = listOf("0", "1", "2", "3", "4", "5", "all").associateWith {
            EventTest::class.java.getResource("/event_kind_$it.txt")!!
                .readText()
                .split("\n")
        }
        private val events: Map<String, List<Event?>> = eventsJson.map {
            it.key to it.value.map {
                Event.fromJson(it)
            }
        }.toMap()

        @JvmStatic fun provideAnyKindEventJson() = eventsJson["all"]!!.stream()
        @JvmStatic fun provideAnyKindEvent() = events["all"]!!.stream()
        @JvmStatic fun provideMetadataEvent() = events[MetadataEvent.kind.toString()]!!.stream()
        @JvmStatic fun provideTextNoteEvent() = events[TextNoteEvent.kind.toString()]!!.stream()
        @JvmStatic fun provideRecommendRelayEvent() = events[RecommendRelayEvent.kind.toString()]!!.stream()
        @JvmStatic fun provideContactListEvent() = events[ContactListEvent.kind.toString()]!!.stream()
        @JvmStatic fun provideEncryptedDmEvent() = events[EncryptedDmEvent.kind.toString()]!!.stream()
        @JvmStatic fun provideDeletionEvent() = events[DeletionEvent.kind.toString()]!!.stream()
    }
}