package nostr.postr

import fr.acinq.secp256k1.Secp256k1
import org.spongycastle.util.encoders.Hex
import java.security.SecureRandom

object Utils {
    fun privkeyCreate(): ByteArray {
        val bytes = ByteArray(32)
        random.nextBytes(bytes)
        return bytes
    }

    fun pubkeyCreate(privKey: ByteArray) =
        secp256k1.pubKeyCompress(secp256k1.pubkeyCreate(privKey)).copyOfRange(1, 33)

    private val secp256k1 = Secp256k1.get()

    private val random = SecureRandom()
}

fun ByteArray.toHex() = String(Hex.encode(this))
