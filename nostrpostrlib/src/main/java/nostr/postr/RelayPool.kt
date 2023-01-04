package nostr.postr

import nostr.postr.events.Event

/**
 * RelayPool manages the connection to multiple Relays and lets consumers deal with simple events.
 */
object RelayPool: Relay.Listener {
    private val relays: MutableList<Relay> = ArrayList()
    private val listeners = HashSet<Listener>()
    private val eventIds = HashSet<String>()


    fun loadRelays(relayList: List<Relay>? = null){
        if (!relayList.isNullOrEmpty()){
            relayList.forEach { addRelay(it) }
        } else {
            Constants.defaultRelays.forEach { addRelay(it) }
        }
    }

    fun sendFilter(subscriptionId: String) {
        relays.forEach { it.connect(subscriptionId = subscriptionId) }

    }

    fun send(signedEvent: Event) {
        relays.forEach { it.sendEvent(signedEvent) }
    }

    fun sendCloseSubscription(subscriptionId: String){
        relays.forEach { it.closeSubscription(subscriptionId) }
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
        fun onNewEvent(event: Event, subscriptionId: String)

        /**
         * A new or repeat message was received
         */

        fun onEvent(event: Event, subscriptionId: String, relay: Relay)

        fun onError(error: Error, subscriptionId: String, relay: Relay)

        fun onRelayStateChange(type: Relay.Type, relay: Relay)
    }

    @Synchronized
    override fun onEvent(relay: Relay, subscriptionId: String, event: Event) {
        listeners.forEach { it.onEvent(event, subscriptionId, relay) }
        synchronized(this) {
            if (eventIds.add(event.id.toHex())) {
                listeners.forEach { it.onNewEvent(event, subscriptionId) }
            }
        }
    }

    override fun onError(relay: Relay, subscriptionId: String, error: Error) {
        listeners.forEach { it.onError(error, subscriptionId, relay) }
    }

    override fun onRelayStateChange(relay: Relay, type: Relay.Type) {
        listeners.forEach { it.onRelayStateChange(type, relay) }
    }

}