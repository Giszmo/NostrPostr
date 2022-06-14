package nostr.postr.examples

import nostr.postr.events.Event
import nostr.postr.toHex
import java.text.SimpleDateFormat
import java.util.*

// helpers
val dateTimeFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)
fun Event.userShort() = pubKey.toHex().substring(0, 8)
fun Event.prettyDate(): String = dateTimeFormat.format(Date(createdAt * 1000))
fun logDetail(event: Event, detail: String) = println("${event.prettyDate()} ${event.userShort()}: $detail")