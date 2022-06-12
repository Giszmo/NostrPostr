package nostr.postr

import org.spongycastle.util.encoders.Hex

object Utils {
    fun byteArrayfromHex(string: String) = Hex.decode(string)
}

fun ByteArray.toHex() = String(Hex.encode(this))
