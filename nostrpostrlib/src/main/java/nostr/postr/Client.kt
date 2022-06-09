package nostr.postr

/**
 * The Nostr Client manages multiple personae the user may switch between. Events are received and
 * published through multiple relays.
 * Events are stored with their respective persona.
 */
object Client: RelayPool.Listener {
    val personae: Array<Persona> = emptyArray()
    private val listeners = HashSet<Listener>()

    fun connect() {
        RelayPool.register(this)
        RelayPool.connect()
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

    fun register(listener: Listener) {
        listeners.add(listener)
    }

    fun unregister(listener: Listener): Boolean {
        return listeners.remove(listener)
    }

    interface Listener {
        /**
         * A new message was received
         */
        fun onEvent(event: Event, relay: Relay)

        /**
         * A new message was received
         */
        fun onNewEvent(event: Event)

        /**
         * A new or repeat message was received
         */

        fun onError(error: Error, relay: Relay)

        /**
         * Connected to or disconnected from a relay
         */
        fun onRelayStateChange(type: Int, relay: Relay)
    }
}