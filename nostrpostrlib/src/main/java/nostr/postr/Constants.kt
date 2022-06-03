package nostr.postr

object Constants {
    val defaultRelays = arrayOf(
        Relay("wss://rsslay.fiatjaf.com", canRead = true, canWrite = false),
        Relay("wss://nostr-pub.wellorder.net", canRead = true, canWrite = true),
        Relay("wss://expensive-relay.fiatjaf.com", canRead = true, canWrite = false)
    )
    val optionalRelays = arrayOf(
        Relay("wss://nostr.rocks", canRead = true, canWrite = true),
        Relay("wss://relayer.fiatjaf.com", canRead = true, canWrite = true),
        Relay("wss://nostr.onsats.org", canRead = true, canWrite = true),
        Relay("wss://nostr-relay.untethr.me	", canRead = true, canWrite = true),
        Relay("wss://nostr-relay.wlvs.space", canRead = true, canWrite = true),
        Relay("wss://nostr.bitcoiner.social", canRead = true, canWrite = true),
        Relay("wss://nostr-relay.freeberty.net", canRead = true, canWrite = true)
    )
}
