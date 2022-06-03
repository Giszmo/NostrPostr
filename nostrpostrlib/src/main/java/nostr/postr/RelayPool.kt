package nostr.postr

/**
 * RelayPool manages the connection to multiple Relays and lets consumers deal with simple events.
 */
object RelayPool: Relay.Listener {
    private val relays: MutableList<Relay> = ArrayList()
    private val listeners = HashSet<Listener>()
    private val events = HashSet<Event>()

    init {
        Constants.defaultRelays.forEach { addRelay(it) }
    }

    fun connect() {
        relays.forEach { it.connect() }
    }

    fun addRelay(relay: Relay) {
        relay.register(this)
        relays += relay
    }

    fun removeRelay(relay: Relay): Boolean {
        relay.unregister(this)
        return relays.remove(relay)
    }

    fun getRelays(): List<Relay> = relays

    fun register(listener: Listener) {
        listeners.add(listener)
    }

    fun unregister(listener: Listener): Boolean {
        return listeners.remove(listener)
    }

    interface Listener {
        /**
         * A new event was received
         */
        fun onNewEvent(event: Event)

        /**
         * A new or repeat message was received
         */

        fun onEvent(event: Event, relay: Relay)

        fun onError(error: Error)

        /**
         * Connected to or disconnected from a relay
         */
        fun onRelayStateChange(type: Int, relay: Relay)
    }

    override fun onEvent(relay: Relay, event: Event) {
        listeners.forEach { it.onEvent(event, relay) }
        if (events.add(event)) {
            listeners.forEach { it.onNewEvent(event) }
        }
    }

    override fun onError(relay: Relay, error: Error) {
        listeners.forEach { it.onError(Error("$error on $relay")) }
    }

    override fun onRelayStateChange(relay: Relay, type: Int) {
        listeners.forEach { it.onRelayStateChange(type, relay) }
    }
}