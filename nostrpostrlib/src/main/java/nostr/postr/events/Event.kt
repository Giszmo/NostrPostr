package nostr.postr

import com.google.gson.*
import de.leowandersleb.lib_bip_schnorr_kotlin.Schnorr
import org.spongycastle.util.encoders.Hex
import java.lang.reflect.Type
import java.security.MessageDigest

open class Event(
    val id: ByteArray,
    val pubkey: ByteArray,
    val created_at: Long,
    val kind: Int,
    val tags: List<List<String>>,
    val content: String,
    val sig: ByteArray
) {
    fun toJson(): String = gson.toJson(this)

    internal fun generateId(): ByteArray {
        val rawEvent = listOf(
            0,
            String(Hex.encode(pubkey)),
            created_at,
            kind,
            tags,
            content
        )
        val rawEventJson = gson.toJson(rawEvent)
        return sha256.digest(rawEventJson.toByteArray())
    }

    fun checkSignature() {
        if (!id.contentEquals(generateId())) {
            throw Error(
                """|Unexpected ID.
                   |  Event: ${toJson()}
                   |  Actual ID: ${String(Hex.encode(id))}
                   |  Generated: ${String(Hex.encode(generateId()))}""".trimIndent()
            )
        }
        Schnorr.verify(id, pubkey, sig)
    }

    class EventDeserializer : JsonDeserializer<Event> {
        override fun deserialize(
            json: JsonElement,
            typeOfT: Type?,
            context: JsonDeserializationContext?
        ): Event {
            val jsonObject = json.asJsonObject
            return Event(
                id = Hex.decode(jsonObject.get("id").asString),
                pubkey = Hex.decode(jsonObject.get("pubkey").asString),
                created_at = jsonObject.get("created_at").asLong,
                kind = jsonObject.get("kind").asInt,
                tags = jsonObject.get("tags").asJsonArray.map {
                    it.asJsonArray.map { s -> s.asString }
                },
                content = jsonObject.get("content").asString,
                sig = Hex.decode(jsonObject.get("sig").asString)
            )
        }
    }

    class EventSerializer : JsonSerializer<Event> {
        override fun serialize(
            src: Event,
            typeOfSrc: Type?,
            context: JsonSerializationContext?
        ): JsonElement {
            return JsonObject().apply {
                addProperty("id", String(Hex.encode(src.id)))
                addProperty("pubkey", String(Hex.encode(src.pubkey)))
                addProperty("created_at", src.created_at)
                addProperty("kind", src.kind)
                add("tags", JsonArray().also { jsonTags ->
                    src.tags.forEach { tag ->
                        jsonTags.add(JsonArray().also { jsonTagElement ->
                            tag.forEach { tagElement ->
                                jsonTagElement.add(tagElement)
                            }
                        })
                    }
                })
                addProperty("content", src.content)
                addProperty("sig", String(Hex.encode(src.sig)))
            }
        }
    }

    class ByteArraySerializer : JsonSerializer<ByteArray> {
        override fun serialize(
            src: ByteArray,
            typeOfSrc: Type?,
            context: JsonSerializationContext?
        ) = JsonPrimitive(String(Hex.encode(src)))
    }

    companion object {
        val sha256: MessageDigest = MessageDigest.getInstance("SHA-256")
        internal val gson = GsonBuilder()
            .disableHtmlEscaping()
            .registerTypeAdapter(Event::class.java, EventSerializer())
            .registerTypeAdapter(Event::class.java, EventDeserializer())
            .registerTypeAdapter(ByteArray::class.java, ByteArraySerializer())
            .create()

        fun fromJson(json: String): Event? = gson.fromJson(json, Event::class.java)
            .run {
                when (kind) {
                    MetadataEvent.kind -> MetadataEvent(id, pubkey, created_at, tags, content, sig)
                    TextNoteEvent.kind -> TextNoteEvent(id, pubkey, created_at, tags, content, sig)
                    RecommendRelayEvent.kind -> RecommendRelayEvent(id, pubkey, created_at, tags, content, sig)
                    ContactListEvent.kind -> ContactListEvent(id, pubkey, created_at, tags, content, sig)
                    EncryptedDmEvent.kind -> EncryptedDmEvent(id, pubkey, created_at, tags, content, sig)
                    DeletionEvent.kind -> DeletionEvent(id, pubkey, created_at, tags, content, sig)
                    else -> null
                }
            }
    }
}
