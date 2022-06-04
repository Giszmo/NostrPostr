package nostr.postr

import com.google.gson.Gson
import de.leowandersleb.lib_bip_schnorr_kotlin.Schnorr

// TODO: How is the ID actually calculated?? https://github.com/nostr-protocol/nips/blob/master/01.md
/*
To obtain the event.id, we sha256 the serialized event. The serialization is done over the UTF-8 JSON-serialized string (with no indentation or extra spaces) of the following structure:

[
  0,
  <pubkey, as a (lowercase) hex string>,
  <created_at, as a number>,
  <kind, as a number>,
  <tags, as an array of arrays of strings>,
  <content, as a string>
]
 */
fun Event.checkId() = Schnorr.sha256("""[0,$pubkey,$createdAt,$kind,${Gson().toJson(tags)},$content]""".toByteArray()).contentEquals(id)

fun Event.checkSignature() = checkId() && Schnorr.verify(id, pubkey, sig)

object Util {
    fun checkSignature(event: Event) = event.checkSignature()

    fun checkId(event: Event) = event.checkId()
}