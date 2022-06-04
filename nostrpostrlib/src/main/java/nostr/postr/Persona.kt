package nostr.postr

class Persona(
    val privateKey: ByteArray?,
    val publicKey: ByteArray?) {
    var petName: String? = null
    val follows: Array<ByteArray>? = null
}