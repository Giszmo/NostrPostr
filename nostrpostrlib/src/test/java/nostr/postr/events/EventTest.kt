package nostr.postr.events

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import org.spongycastle.util.encoders.Hex
import java.lang.Exception
import java.util.stream.Collectors
import java.util.stream.Stream
import kotlin.streams.toList

class EventTest {
    @ParameterizedTest @MethodSource("provideAnyKindEventJson")
    fun fromToJson(eventJson: String) {
        check(eventJson.isNotEmpty())
        // val eventJsonActual =
        Event.fromJson(eventJson).toJson()
        // The order of event members is not determined or relevant.
        // Reproducibility of the serialized form is not required.
        // assertEquals(eventJson, eventJsonActual, "Actual events don't survive deserialization and re-serialization")
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

    @ParameterizedTest @MethodSource("provideRecommendRelayEventLenient")
    fun checkRecommendRelayEventLenient(eventJson: String) {
        val event = Event.fromJson(eventJson, true)
        assertTrue(event is RecommendRelayEvent)
        assertThrowsExactly(IllegalArgumentException::class.java) {
            Event.fromJson(eventJson, false)
        }
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
        private fun eventsJson(name: String): Stream<String> =
            EventTest::class.java.getResource("/event_kind_$name.txt")!!
                .readText().split("\n").stream()

        private fun events(name: String): Stream<Event> =
                    eventsJson(name).map(Event.Companion::fromJson)

        @JvmStatic fun provideAnyKindEventJson() = eventsJson("all")
        @JvmStatic fun provideAnyKindEvent() = events("all")
        @JvmStatic fun provideMetadataEvent() = events(MetadataEvent.kind.toString())
        @JvmStatic fun provideTextNoteEvent() = events(TextNoteEvent.kind.toString())
        @JvmStatic fun provideRecommendRelayEvent() = events(RecommendRelayEvent.kind.toString())
        @JvmStatic fun provideRecommendRelayEventLenient() = eventsJson("2_lenient")
        @JvmStatic fun provideContactListEvent() = events(ContactListEvent.kind.toString())
        @JvmStatic fun provideEncryptedDmEvent() = events(EncryptedDmEvent.kind.toString())
        @JvmStatic fun provideDeletionEvent() = events(DeletionEvent.kind.toString())

        @JvmStatic fun provideAnyKindEventAll(): Stream<Event> {
            return listOf("0", "1", "2", "3", "4", "5", "6", "7", "17", "30", "all")
                .flatMap {
                    events(it).collect(Collectors.toList())
                }
                .stream()
        }
    }
}