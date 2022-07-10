package nostr.postr

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

internal class UtilsKtTest {
    @Test
    fun intToByteArray() {
        listOf(0, 1, 16, 256, 15555, Int.MAX_VALUE, Int.MIN_VALUE).forEach {
            println("$it in hex is ${it.toByteArray().toHex()}.")
        }
    }
}