import com.daveanthonythomas.moshipack.FormatInterchange
import com.daveanthonythomas.moshipack.MoshiPack
import com.squareup.moshi.MsgpackFormat
import okio.Buffer
import org.junit.Assert.assertEquals
import org.junit.Test
import org.msgpack.core.MessagePack

class TestAgainstMessagePackJava {

    @Test
    fun comparison() {
        val packer = MessagePack.newDefaultBufferPacker()
        packer.packMapHeader(2)
        packer.packString("compact")
        packer.packBoolean(true)
        packer.packString("schema")
        packer.packShort(0)

        val messagePackResult = Buffer().apply { write(packer.toByteArray()) }
        val moshiPackResult = MoshiPack.pack(ThePlug())

        assertEquals(messagePackResult.readByteString().hex(), moshiPackResult.readByteString().hex())
    }

    @Test
    fun comparisonNegative() {
        val packer = MessagePack.newDefaultBufferPacker()
        packer.packMapHeader(2)
        packer.packString("compact")
        packer.packBoolean(true)
        packer.packString("schema")
        packer.packShort(-9)

        val messagePackResult = Buffer().apply { write(packer.toByteArray()) }
        val moshiPackResult = MoshiPack.pack(ThePlug().apply { schema = -9 })

        assertEquals(messagePackResult.readByteString().hex(), moshiPackResult.readByteString().hex())
    }

    @Test
    fun comparisonNegativeOneByteMax() {
        val packer = MessagePack.newDefaultBufferPacker()
        packer.packMapHeader(2)
        packer.packString("compact")
        packer.packBoolean(true)
        packer.packString("schema")
        packer.packShort(-32)

        val messagePackResult = Buffer().apply { write(packer.toByteArray()) }
        val moshiPackResult = MoshiPack.pack(ThePlug().apply { schema = -32 })

        assertEquals(messagePackResult.readByteString().hex(), moshiPackResult.readByteString().hex())
    }

    @Test
    fun comparisonBoolean() {
        val packer = MessagePack.newDefaultBufferPacker()
        packer.packMapHeader(2)
        packer.packString("stuffed")
        packer.packBoolean(false)
        packer.packString("topping")
        packer.packString("Stuff")

        val messagePackResult = Buffer().apply { write(packer.toByteArray()) }
        val moshiPackResult = MoshiPack.pack(PizzaPlus().apply { stuffed = false })

        assertEquals(messagePackResult.readByteString().hex(), moshiPackResult.readByteString().hex())
    }

    @Test
    fun comparisonUInt8() {
        val packer = MessagePack.newDefaultBufferPacker()
        packer.packMapHeader(2)
        packer.packString("compact")
        packer.packBoolean(true)
        packer.packString("schema")
        packer.packShort(200)

        val messagePackResult = Buffer().apply { write(packer.toByteArray()) }
        val moshiPackResult = MoshiPack.pack(ThePlug().apply { schema = 200 })

        assertEquals(messagePackResult.readByteString().hex(), moshiPackResult.readByteString().hex())
    }

    @Test
    fun comparisonUInt16() {
        val packer = MessagePack.newDefaultBufferPacker()
        packer.packMapHeader(2)
        packer.packString("compact")
        packer.packBoolean(true)
        packer.packString("schema")
        packer.packShort(32767)

        val messagePackResult = Buffer().apply { write(packer.toByteArray()) }
        val moshiPackResult = MoshiPack.pack(ThePlug().apply { schema = 32767 })

        assertEquals(messagePackResult.readByteString().hex(), moshiPackResult.readByteString().hex())
    }

    @Test
    fun comparisonNegativeTwoByte() {
        val packer = MessagePack.newDefaultBufferPacker()
        packer.packMapHeader(2)
        packer.packString("compact")
        packer.packBoolean(true)
        packer.packString("schema")
        packer.packShort(-202)

        val messagePackResult = Buffer().apply { write(packer.toByteArray()) }
        val moshiPackResult = MoshiPack.pack(ThePlug().apply { schema = -202 })

        assertEquals(messagePackResult.readByteString().hex(), moshiPackResult.readByteString().hex())
    }

    @Test
    fun comparisonNegativeFourByte() {
        val packer = MessagePack.newDefaultBufferPacker()
        packer.packMapHeader(2)
        packer.packString("compact")
        packer.packBoolean(true)
        packer.packString("schema")
        packer.packInt(-202090)

        val messagePackResult = Buffer().apply { write(packer.toByteArray()) }
        val moshiPackResult = MoshiPack.pack(ThePlug().apply { schema = -202090 })

        assertEquals(messagePackResult.readByteString().hex(), moshiPackResult.readByteString().hex())
    }

    @Test
    fun comparisonNegativeEightByte() {
        val packer = MessagePack.newDefaultBufferPacker()
        packer.packMapHeader(2)
        packer.packString("compact")
        packer.packBoolean(true)
        packer.packString("schema")
        packer.packLong(-2020900943243289)

        val messagePackResult = Buffer().apply { write(packer.toByteArray()) }
        val moshiPackResult = MoshiPack.jsonToMsgpack("{'compact':true,'schema':-2020900943243289}")

        assertEquals(messagePackResult.readByteString().hex(), moshiPackResult.readByteString().hex())
    }

    @Test
    fun comparisonBigPositiveNumber() {
        val packer = MessagePack.newDefaultBufferPacker()
        packer.packMapHeader(1)
        packer.packString("bigNumber")
        packer.packLong(2020900943243289543)

        data class TestClass(var bigNumber: Long = 2020900943243289543)

        val messagePackResult = Buffer().apply { write(packer.toByteArray()) }
        val moshiPackResult = MoshiPack.pack(TestClass())

        assertEquals(messagePackResult.readByteString().hex(), moshiPackResult.readByteString().hex())
    }

    @Test
    fun comparisonFloatingPoint() {
        val packer = MessagePack.newDefaultBufferPacker()
        packer.packMapHeader(1)
        packer.packString("float")
        packer.packFloat(202.202F)

        data class TestClass(var float: Float = 202.202F)

        val messagePackResult = Buffer().apply { write(packer.toByteArray()) }
        val moshiPackResult = MoshiPack.pack(TestClass())

        assertEquals(messagePackResult.readByteString().hex(), moshiPackResult.readByteString().hex())
    }

    @Test
    fun string() {
        val string = "Nice little string value!"
        val packer = MessagePack.newDefaultBufferPacker()
        packer.packString(string)

        val messagePackResult = Buffer().apply { write(packer.toByteArray()) }
        val moshiPackResult = MoshiPack.pack(string)

        assertEquals(messagePackResult.readByteString().hex(), moshiPackResult.readByteString().hex())
    }

    @Test
    fun stringDecode() {
        val string = "Nice little string value!"
        val packer = MessagePack.newDefaultBufferPacker()
        packer.packString(string)

        val moshiPackResult: String = MoshiPack.unpack(packer.toByteArray())

        assertEquals(string, moshiPackResult)
    }

    @Test
    fun stringLong() {
        val string = "Nice little string value! Lorem Ipsum is simply dummy text of the printing and typesetting industry. Lorem Ipsum has been the industry's standard dummy text ever since the 1500s, when an unknown printer took a galley of type and scrambled it to make a type specimen book. It has survived not only five centuries, but also the leap into electronic typesetting, remaining essentially unchanged. It was popularised in the 1960s with the release of Letraset sheets containing Lorem Ipsum passages, and more recently with desktop publishing software like Aldus PageMaker including versions of Lorem Ipsum."
        val packer = MessagePack.newDefaultBufferPacker()
        packer.packString(string)

        val messagePackResult = Buffer().apply { write(packer.toByteArray()) }
        val moshiPackResult = MoshiPack.pack(string)

        assertEquals(messagePackResult.readByteString().hex(), moshiPackResult.readByteString().hex())
    }

    @Test
    fun stringDecodeLong() {
        val string = "Nice little string value! Lorem Ipsum is simply dummy text of the printing and typesetting industry. Lorem Ipsum has been the industry's standard dummy text ever since the 1500s, when an unknown printer took a galley of type and scrambled it to make a type specimen book. It has survived not only five centuries, but also the leap into electronic typesetting, remaining essentially unchanged. It was popularised in the 1960s with the release of Letraset sheets containing Lorem Ipsum passages, and more recently with desktop publishing software like Aldus PageMaker including versions of Lorem Ipsum."
        val packer = MessagePack.newDefaultBufferPacker()
        packer.packString(string)

        val moshiPackResult: String = MoshiPack.unpack(packer.toByteArray())

        assertEquals(string, moshiPackResult)
    }

    @Test
    fun string8() {
        val string = "1234567890123456789012345678901234567890"
        val packer = MessagePack.newDefaultBufferPacker()
        packer.packString(string)

        val messagePackResult = Buffer().apply { write(packer.toByteArray()) }
        val moshiPackResult = MoshiPack.pack(string)

        assertEquals(messagePackResult.readByteString().hex(), moshiPackResult.readByteString().hex())
    }

    @Test
    fun string8Decode() {
        val string = "1234567890123456789012345678901234567890"
        val packer = MessagePack.newDefaultBufferPacker()
        packer.packString(string)

        val moshiPackResult: String = MoshiPack.unpack(packer.toByteArray())

        assertEquals(string, moshiPackResult)
    }

    @Test
    fun string32Decode() {
        val longString = StringBuilder("Long long string")
        (0..MsgpackFormat.SIZE_16).forEach { longString.append("a") }
        val packer = MessagePack.newDefaultBufferPacker()
        packer.packString(longString.toString())

        val moshiPackResult: String = MoshiPack.unpack(packer.toByteArray())

        assertEquals(longString.toString(), moshiPackResult)
    }
}