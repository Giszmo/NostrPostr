package nostr.postr

object Constants {
    val defaultRelays = arrayOf(
        Relay("wss://rsslay.fiatjaf.com", read = true, write = false),
        Relay("wss://nostr-pub.wellorder.net", read = true, write = true),
        Relay("wss://expensive-relay.fiatjaf.com", read = true, write = false)
    )
    val optionalRelays = arrayOf(
        Relay("wss://nostr.rocks", read = true, write = true),
        Relay("wss://relayer.fiatjaf.com", read = true, write = true),
        Relay("wss://nostr.onsats.org", read = true, write = true),
        Relay("wss://nostr-relay.untethr.me	", read = true, write = true),
        Relay("wss://nostr-relay.wlvs.space", read = true, write = true),
        Relay("wss://nostr.bitcoiner.social", read = true, write = true),
        Relay("wss://nostr-relay.freeberty.net", read = true, write = true)
    )
}
