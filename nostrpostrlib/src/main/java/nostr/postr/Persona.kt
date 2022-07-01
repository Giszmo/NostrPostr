package nostr.postr

class Persona(
    privateKey: ByteArray? = null,
    publicKey: ByteArray? = null
) {
    val privateKey: ByteArray?
    val publicKey: ByteArray
    var petName: String? = null
    val follows: Array<ByteArray>? = null

    init {
        if (privateKey == null) {
            if (publicKey == null) {
                // create new, random keys
                this.privateKey = Utils.privkeyCreate()
                this.publicKey = Utils.pubkeyCreate(this.privateKey)
            } else {
                // this is a read-only account
                check(publicKey.size == 32)
                this.privateKey = null
                this.publicKey = publicKey
            }
        } else {
            // as private key is provided, ignore the public key and set keys according to private key
            this.privateKey = privateKey
            this.publicKey = Utils.pubkeyCreate(privateKey)
        }
    }

    override fun toString(): String {
        return "Persona(privateKey=${privateKey?.toHex()}, publicKey=${publicKey.toHex()}, petName=$petName, follows=${follows?.contentToString()})"
    }
}
