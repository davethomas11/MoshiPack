import com.daveanthonythomas.moshipack.FormatInterchange
import com.daveanthonythomas.moshipack.MoshiPack
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
        val moshiPackResult = MoshiPack().jsonToMsgpack("{'compact':true,'schema':-2020900943243289}")

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
}