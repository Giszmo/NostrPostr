package nostr.postr

object Constants {
    val defaultRelays = arrayOf(
        Relay("wss://nostr-relay.untethr.me", read = true, write = true),
        Relay("wss://nostr-relay.freeberty.net", read = true, write = true),
        Relay("wss://nostr.bitcoiner.social", read = true, write = true),
        Relay("wss://nostr-relay.wlvs.space", read = true, write = true),
        Relay("wss://nostr-pub.wellorder.net", read = true, write = true),
        Relay("wss://nostr.rocks", read = true, write = true),
        Relay("wss://nostr.onsats.org", read = true, write = true)
    )
}
