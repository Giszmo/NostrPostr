package nostr.postr

import nostr.postr.events.Event

/**
 * The Nostr Client manages multiple personae the user may switch between. Events are received and
 * published through multiple relays.
 * Events are stored with their respective persona.
 */
object Client: RelayPool.Listener {
    val personae: Array<Persona> = emptyArray()
    private val listeners = HashSet<Listener>()
    internal var filters: MutableList<String> = mutableListOf("{}")
    internal var relays = Constants.defaultRelays

    fun connect(
        filters: MutableList<String> = mutableListOf("{}"),
        relays: Array<Relay> = Constants.defaultRelays) {
        this.filters = filters
        this.relays = relays
        RelayPool.register(this)
        RelayPool.connect()
    }

    fun disconnect() {
        RelayPool.unregister(this)
        RelayPool.disconnect()
    }

    fun addFilter(filter: String) {
        filters.add(filter)
        RelayPool.sendFilter()
    }

    override fun onEvent(event: Event, relay: Relay) {
        listeners.forEach { it.onEvent(event, relay) }
    }

    override fun onNewEvent(event: Event) {
        listeners.forEach { it.onNewEvent(event) }
    }

    override fun onError(error: Error, relay: Relay) {
        listeners.forEach { it.onError(error, relay) }
    }

    override fun onRelayStateChange(type: Int, relay: Relay) {
        listeners.forEach { it.onRelayStateChange(type, relay) }
    }

    fun subscribe(listener: Listener) {
        listeners.add(listener)
    }

    fun unsubscribe(listener: Listener): Boolean {
        return listeners.remove(listener)
    }

    abstract class Listener {
        /**
         * A new message was received
         */
        open fun onEvent(event: Event, relay: Relay) = Unit

        /**
         * A new message was received
         */
        open fun onNewEvent(event: Event) = Unit

        /**
         * A new or repeat message was received
         */

        open fun onError(error: Error, relay: Relay) = Unit

        /**
         * Connected to or disconnected from a relay
         */
        open fun onRelayStateChange(type: Int, relay: Relay) = Unit
    }
}