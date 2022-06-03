package nostr.postr

import java.security.PrivateKey
import java.security.PublicKey

class Persona(val privateKey: PrivateKey?, val publicKey: PublicKey?) {
    var petName: String? = null
    val follows: Array<PublicKey>? = null

}