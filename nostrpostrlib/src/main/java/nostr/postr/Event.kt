package nostr.postr

data class Event(
    val id: ByteArray,
    val pubkey: ByteArray,
    val createdAt: Long,
    val kind: Int,
    val tags: Array<Any>,
    val content: String,
    val sig: ByteArray
)
// TODO: split by kinds.
/*
    0: set_metadata: the content is set to a stringified JSON object {name: <string>, about: <string>, picture: <url, string>} describing the user who created the event. A relay may delete past set_metadata events once it gets a new one for the same pubkey.
    1: text_note: the content is set to the text content of a note (anything the user wants to say).
    2: recommend_server: the content is set to the URL (e.g., https://somerelay.com) of a relay the event creator wants to recommend to its followers.
*/
