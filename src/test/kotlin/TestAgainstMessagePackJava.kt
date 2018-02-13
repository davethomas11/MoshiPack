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
}