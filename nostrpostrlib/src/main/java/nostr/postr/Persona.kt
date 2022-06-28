package nostr.postr

import fr.acinq.secp256k1.Secp256k1
import nostr.postr.events.Event
import nostr.postr.events.Event.Companion.getRefinedEvent

class Persona(
    var privateKey: ByteArray? = null,
    var publicKey: ByteArray? = null
) {

    init {
        if (privateKey == null) {
            if (publicKey == null) {
                privateKey = Utils.privkeyCreate()
                publicKey = Utils.pubkeyCreate(privateKey!!)
            } else {
                check(publicKey!!.size == 32)
            }
        } else {
            publicKey = Utils.pubkeyCreate(privateKey!!)
        }
    }

    var petName: String? = null
    val follows: Array<ByteArray>? = null

    override fun toString(): String {
        return "Persona(privateKey=${privateKey?.toHex()}, publicKey=${publicKey?.toHex()}, petName=$petName, follows=${follows?.contentToString()})"
    }
}
