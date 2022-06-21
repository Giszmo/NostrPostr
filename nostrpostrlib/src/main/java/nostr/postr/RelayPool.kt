package nostr.postr

import nostr.postr.events.Event

/**
 * RelayPool manages the connection to multiple Relays and lets consumers deal with simple events.
 */
object RelayPool: Relay.Listener {
    private val relays: MutableList<Relay> = ArrayList()
    private val listeners = HashSet<Listener>()
    private val eventIds = HashSet<String>()

    init {
        Constants.defaultRelays.forEach { addRelay(it) }
    }

    fun connect() {
        relays.forEach { it.connect() }
    }

    fun disconnect() {
        relays.forEach { it.disconnect() }
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

        fun onError(error: Error, relay: Relay)

        fun onRelayStateChange(type: Relay.Type, relay: Relay)
    }

    @Synchronized
    override fun onEvent(relay: Relay, event: Event) {
        listeners.forEach { it.onEvent(event, relay) }
        synchronized(this) {
            if (eventIds.add(event.id.toHex())) {
                listeners.forEach { it.onNewEvent(event) }
            }
        }
    }

    override fun onError(relay: Relay, error: Error) {
        listeners.forEach { it.onError(error, relay) }
    }

    override fun onRelayStateChange(relay: Relay, type: Relay.Type) {
        listeners.forEach { it.onRelayStateChange(type, relay) }
    }

    fun sendFilter() {
        relays.forEach(Relay::sendFilter)
    }
}