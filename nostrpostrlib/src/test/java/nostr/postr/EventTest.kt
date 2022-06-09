package nostr.postr

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import org.spongycastle.util.encoders.Hex
import java.util.stream.Stream

class EventTest {
    @ParameterizedTest
    @MethodSource("provideEventJson")
    fun fromToJson(eventJson: String) {
        val event = Event.fromJson(eventJson)
        val eventJsonActual = event.toJson()
        assertEquals(eventJson, eventJsonActual, "Actual events don't survive deserialization and re-serialization")
    }

    @Test
    fun generateId() {
        val event = Event(
            Hex.decode("771da2d40c56fab8d20a8678fb28f2b858446d6d5da31511939a4ebe1a247210"),
            Hex.decode("32e1827635450ebb3c5a7d12c1f8e7b2b514439ac10a67eef3d9fd9c5c68e245"),
            1650975065,
            1,
            emptyList(),
            "idea: pay for PoW over nostr:\n\nSend note to a paid pow relay where you register your pubkey. The contents of this is a json-encoded unsigned note that you want PoW on.\n\nService decodes this note, does PoW, sends it back to relays encrypted. Client sees this, decrypts, checks it against the original note sent (that is encoded in the response for convenience), signs it, and broadcasts.",
            Hex.decode("f3146129c4e55ae0e41738b7d3d66f67602ff5e75b9845ec1cf7cfc3ae43c59d6cf821ed97b9b03d5bf3f453d4359970b024feaae9a221c547619526de250539")
        )
        val generatedId = event.generateId()
        assertEquals(String(Hex.encode(event.id)), String(Hex.encode(generatedId)))
    }

    @Test
    fun getPubkey() {
    }

    @Test
    fun getCreatedAt() {
    }

    @Test
    fun getKind() {
    }

    @Test
    fun getTags() {
    }

    @Test
    fun getContent() {
    }

    @Test
    fun getSig() {
    }

    companion object {
        @JvmStatic
        fun provideEventJson(): Stream<Arguments> {
            return EventTest::class.java.getResource("/test_relay_events.txt")!!
                .readText()
                .split("\n")
                .map { Arguments.of( it.substring(24, it.length - 1)) }
                .stream()
        }
    }
}