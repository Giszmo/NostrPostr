package nostr.postr

class Persona(
    privKey: ByteArray? = null,
    pubKey: ByteArray? = null
) {
    val privKey: ByteArray?
    val pubKey: ByteArray
    var petName: String? = null
    val follows: Array<ByteArray>? = null

    init {
        if (privKey == null) {
            if (pubKey == null) {
                // create new, random keys
                this.privKey = Utils.privkeyCreate()
                this.pubKey = Utils.pubkeyCreate(this.privKey)
            } else {
                // this is a read-only account
                check(pubKey.size == 32)
                this.privKey = null
                this.pubKey = pubKey
            }
        } else {
            // as private key is provided, ignore the public key and set keys according to private key
            this.privKey = privKey
            this.pubKey = Utils.pubkeyCreate(privKey)
        }
    }

    override fun toString(): String {
        return "Persona(privateKey=${privKey?.toHex()}, publicKey=${pubKey.toHex()}, petName=$petName, follows=${follows?.contentToString()})"
    }
}
