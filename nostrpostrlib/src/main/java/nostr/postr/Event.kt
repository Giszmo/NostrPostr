package nostr.postr

import com.google.gson.*
import de.leowandersleb.lib_bip_schnorr_kotlin.Schnorr
import org.spongycastle.util.encoders.Hex
import java.lang.reflect.Type
import java.security.MessageDigest

open class Event(
    val id: ByteArray,
    val pubkey: ByteArray,
    val createdAt: Long,
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
            createdAt,
            kind,
            tags,
            content)
        val rawEventJson = gson.toJson(rawEvent)
        val sha256 = MessageDigest.getInstance("SHA-256")
        val hash = sha256.digest(rawEventJson.toByteArray())
        return hash
    }

    private fun checkSignature() {
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
                createdAt = jsonObject.get("created_at").asLong,
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
                addProperty("created_at", src.createdAt)
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

    companion object {
        private var gson = GsonBuilder()
            .disableHtmlEscaping()
            .registerTypeAdapter(Event::class.java, EventSerializer())
            .registerTypeAdapter(Event::class.java, EventDeserializer())
            .create()

        fun fromJson(json: String): Event {
            val event = gson.fromJson(json, Event::class.java)
            event.checkSignature()
            return event
        }
    }
}

// data class MetaDataEvent(): Event()
// TODO: split by kinds.
/*
    0: set_metadata: the content is set to a stringified JSON object {name: <string>, about: <string>, picture: <url, string>} describing the user who created the event. A relay may delete past set_metadata events once it gets a new one for the same pubkey.
    1: text_note: the content is set to the text content of a note (anything the user wants to say).
    2: recommend_server: the content is set to the URL (e.g., https://somerelay.com) of a relay the event creator wants to recommend to its followers.
*/
