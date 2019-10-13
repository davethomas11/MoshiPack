import com.daveanthonythomas.moshipack.MoshiPack
import com.squareup.moshi.Moshi
import com.squareup.moshi.MsgpackFormat
import com.squareup.moshi.MsgpackWriter
import okio.Buffer
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Test


class TestMessagePackWriter {

    @Test
    fun seeIfThisThingWorksOrNot() {
        val moshi = Moshi.Builder().build()
        val buffer = Buffer()
        moshi.adapter(Pizza::class.java).toJson(MsgpackWriter(buffer), Pizza())

        val msgPackString = buffer.readByteString()

        // First test
        // Expect fix map 81 with size of 1
        // Then fix string a7 with size of 7 bytes ( topping ) 74 6f 70 70 69 6e 67
        // Then fix string a5 with size of 5 bytes ( Stuff ) 53 74 75 66 66

        assertEquals("81a7746f7070696e67a55374756666", msgPackString.hex())
    }

    @Test
    fun okOneMoreTestThanYouGottaGotoSleep() {
        val moshi = Moshi.Builder().build()
        val buffer = Buffer()
        moshi.adapter(PizzaPlus::class.java).toJson(MsgpackWriter(buffer), PizzaPlus())

        val msgPackString = buffer.readByteString()

        // Second test
        // Expect fix map 82 with size of 2
        // Then fix string a7 with size of 7 bytes ( topping ) 74 6f 70 70 69 6e 67
        // Then fix string a5 with size of 5 bytes ( Stuff ) 53 74 75 66 66
        // Then fix string a7 with size of 5 bytes ( stuffed ) 53 74 75 66 66
        // Then boolean true

        // Well shit... s comes before t so I think stuffed goes first
        // should be this actually:

        // Expect fix map 81 with size of 2
        // Then fix string a7 with size of 5 bytes ( stuffed ) 73 74 75 66 66 65 64
        // Then boolean true c3
        // Then fix string a7 with size of 7 bytes ( topping ) 74 6f 70 70 69 6e 67
        // Then fix string a5 with size of 5 bytes ( Stuff ) 53 74 75 66 66


        assertEquals("82a773747566666564c3a7746f7070696e67a55374756666", msgPackString.hex())
    }


    @Test
    fun whoOrdersJustOnePizzaNotMe() {
        val pizzas = listOf(Pizza(), Pizza())

        val buffer = MoshiPack.pack(pizzas)

        val pizzabytes = "81a7746f7070696e67a55374756666"

        assertEquals("92$pizzabytes$pizzabytes", buffer.readByteString().hex())
    }

    @Test
    fun theMessagePackWebsitePlug() {
//        It's like JSON.
//        but fast and small.
        //https://msgpack.org/images/intro.png

        // 82 - 2 element map
        // a7 63 6f 6d 70 61 63 74 - compact
        // c3 - true

        // a6 73 63 68 65 6d 61 - schema
        // 00 - integer 0

        val map = mapOf("compact" to true, "schema" to 0)
        val buffer = MoshiPack.pack(map)

        assertEquals("82a7636f6d70616374c3a6736368656d6100", buffer.readByteString().hex())
    }

    @Test
    fun transientsAreNotWritten() {
        val transients = Transients("A", "C").apply { two = "B" }
        val buffer = MoshiPack().pack(transients)

        assertEquals("82a3${"one".hex}a141a5${"three".hex}a143", buffer.readByteString().hex())
    }

    @Test
    fun transientsAreNotWrittenMoshiNoKotlinSupport() {
        val transients = Transients2("A", "B", "C")
        val buffer = MoshiPack(moshi = Moshi.Builder().build()).pack(transients)

        assertEquals("82a3${"one".hex}a141a5${"three".hex}a143", buffer.readByteString().hex())
    }

    @Test
    fun fixedUint64WorksWithZero() {
        // Single uInt64 value
        val expected = byteArrayOf(
            0xcf.toByte(), 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00
        )
        val bytes =
            MoshiPack(moshi = Moshi.Builder().build()).packToByteArray(0x00, MsgpackFormat.UINT_64)
        assertArrayEquals(expected, bytes)
    }

    @Test
    fun fixedUint64WorksWithMaxLong() {
        // uInt64 format byte followed by Long.MAX_VALUE in bytes
        val expected = byteArrayOf(
            0xcf.toByte(),
            0x7F, 0xFF.toByte(), 0xFF.toByte(), 0xFF.toByte(),
            0xFF.toByte(), 0xFF.toByte(), 0xFF.toByte(), 0xFF.toByte()
        )
        val bytes = MoshiPack(moshi = Moshi.Builder().build()).packToByteArray(
            Long.MAX_VALUE,
            MsgpackFormat.UINT_64
        )
        assertArrayEquals(expected, bytes)
    }

    @Test
    fun fixedUint64WorksWithMaxInt() {
        // uInt64 format byte value followed by value Int.MAX_VALUE in bytes
        val expected = byteArrayOf(
            0xcf.toByte(),
            0x00, 0x00, 0x00, 0x00, 0x7F, 0xFF.toByte(), 0xFF.toByte(), 0xFF.toByte()
        )
        val bytes = MoshiPack(moshi = Moshi.Builder().build()).packToByteArray(
            Int.MAX_VALUE,
            MsgpackFormat.UINT_64
        )
        assertArrayEquals(expected, bytes)
    }

    @Test
    fun fixedUint64WorksWithSingleElementArray() {
        // fixarray format byte, uInt64 format byte, zero value bytes
        val expected = byteArrayOf(
            0x91.toByte(),
            0xcf.toByte(), 0x00, 0x00, 0x00, 0x00,
            0x00, 0x00, 0x00, 0x00
        )
        val bytes = MoshiPack(moshi = Moshi.Builder().build()).packToByteArray(
            byteArrayOf(0x00),
            MsgpackFormat.UINT_64
        )
        assertArrayEquals(expected, bytes)
    }

    @Test
    fun arrayOfUint64WorksWithFixedSize() {
        // fixarray format byte, uInt64 format byte, value bytes, uInt64 format byte, value bytes
        val expected = byteArrayOf(
            0x92.toByte(),
            0xcf.toByte(), 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x01,
            0xcf.toByte(), 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x01
        )
        val bytes = MoshiPack(moshi = Moshi.Builder().build()).packToByteArray(
            byteArrayOf(0x01, 0x01),
            MsgpackFormat.UINT_64
        )
        assertArrayEquals(expected, bytes)
    }

    @Test
    fun mapOfUint64WorksWithFixedSize() {
        val expected = byteArrayOf(
            0x82.toByte(),
            0xA1.toByte(), 0x31,
            0xcf.toByte(), 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x03,
            0xA1.toByte(), 0x32,
            0xcf.toByte(), 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x04
        )
        val bytes = MoshiPack(moshi = Moshi.Builder().build()).packToByteArray(
            mapOf(1 to 0x03, 2 to 0x04),
            MsgpackFormat.UINT_64
        )
        assertArrayEquals(expected, bytes)
    }


    @Test
    fun mapOfSmallestSizeWithSingleElement() {
        val expected = byteArrayOf(-127, -95, 49, 3)
        val bytes = MoshiPack(moshi = Moshi.Builder().build()).packToByteArray(
            mapOf(1 to 0x03)
        )
        assertArrayEquals(expected, bytes)
    }

    @Test
    fun mapOfSmallestSizeWithTwoElement() {
        val expected = byteArrayOf(-126, -95, 49, 3, -95, 50, 4)
        val bytes = MoshiPack(moshi = Moshi.Builder().build()).packToByteArray(
            mapOf(1 to 0x03, 2 to 0x04)
        )
        assertArrayEquals(expected, bytes)
    }
}
