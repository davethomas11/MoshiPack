import com.daveanthonythomas.moshipack.MoshiPack
import com.squareup.moshi.Moshi
import com.squareup.moshi.MsgpackFormat
import com.squareup.moshi.MsgpackReader
import com.sun.xml.internal.ws.org.objectweb.asm.ClassAdapter
import okio.Buffer
import okio.ByteString
import org.hamcrest.CoreMatchers
import org.hamcrest.CoreMatchers.hasItems
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThat
import org.junit.Test

class TestMessagePackReader {

    @Test
    fun doesMyReaderEvenWorkYet() {
        val moshi = Moshi.Builder().build()
        val buffer = Buffer()

        // First test
        // Expect fix map 81 with size of 1
        // Then fix string a7 with size of 7 bytes ( topping ) 74 6f 70 70 69 6e 67
        // Then fix string a9 with size of 9 bytes ( pepperoni" ) 70 65 70 70 65 72 6f 6e 69
        buffer.write(ByteString.decodeHex("81a7746f7070696e67a97065707065726f6e69"))
        val pizza = moshi.adapter(Pizza::class.java).fromJson(MsgpackReader(buffer))

        assertEquals(pizza?.topping, "pepperoni")
    }

    @Test
    fun didSomeoneOrderSomePizzas() {

        val pizzabytes = "81a7746f7070696e67a97065707065726f6e69" // Not bagel bytes?

        val buffer = Buffer()
        buffer.write(ByteString.decodeHex("93$pizzabytes$pizzabytes$pizzabytes"))

        val pizzas: List<Pizza> = MoshiPack.unpack(buffer)

        assertEquals(3, pizzas.size)

        assertThat(pizzas, hasItems(
                Pizza("pepperoni"),
                Pizza("pepperoni"),
                Pizza("pepperoni")))
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

        val buffer = Buffer()
        buffer.write(ByteString.decodeHex("82a7636f6d70616374c3a6736368656d6100"))

        val unpacked: Map<Any, Any> = MoshiPack.unpack(buffer)

        assertEquals(2, unpacked.size)
        assertEquals(true, unpacked["compact"])
        assertEquals(0.0, unpacked["schema"])
    }

    @Test
    fun theMessagePackWebsitePlugObj() {
//        It's like JSON.
//        but fast and small.
        //https://msgpack.org/images/intro.png

        // 82 - 2 element map
        // a7 63 6f 6d 70 61 63 74 - compact
        // c3 - true

        // a6 73 63 68 65 6d 61 - schema
        // 00 - integer 0

        val buffer = Buffer()
        buffer.write(ByteString.decodeHex("82a7636f6d70616374c3a6736368656d6100"))

        val unpacked: ThePlug = MoshiPack.unpack(buffer)

        assertEquals(true, unpacked.compact)
        assertEquals(0, unpacked.schema)
    }

    @Test
    fun nestObjects() {

        val buffer = Buffer()
        buffer += "81"
        buffer += "a4${"eggs".hex}"
        buffer += "95"
        (1 until 6).forEach {
            buffer += "81"
            buffer += "a4${"size".hex}"
            buffer.writeByte(it)
        }

        val nest: Nest = MoshiPack.unpack(buffer)

        assertEquals(5, nest.eggs.size)

        (0 until 5).forEach {
            assertEquals(it + 1, nest.eggs[it].size)
        }
    }

    @Test
    fun testList() {

        val buffer = Buffer()
        buffer += "92"

        buffer += "81"
        buffer += "a4${"eggs".hex}"
        buffer += "95"
        (1 until 6).forEach {
            buffer += "81"
            buffer += "a4${"size".hex}"
            buffer.writeByte(it)
        }

        buffer += "81"
        buffer += "a4${"eggs".hex}"
        buffer += "95"
        (1 until 6).forEach {
            buffer += "81"
            buffer += "a4${"size".hex}"
            buffer.writeByte(it)
        }

        val list: List<Nest> = MoshiPack().unpack(buffer)
        val nest: Nest = list[0]

        assertEquals(nest.eggs[0].size, 1)
    }

    @Test
    fun listAny() {
        val buffer = Buffer()
        buffer += "92"

        buffer += "81"
        buffer += "a4${"eggs".hex}"
        buffer += "95"
        (1 until 6).forEach {
            buffer += "81"
            buffer += "a4${"size".hex}"
            buffer.writeByte(it)
        }

        buffer += "81"
        buffer += "a4${"eggs".hex}"
        buffer += "95"
        (1 until 6).forEach {
            buffer += "81"
            buffer += "a4${"size".hex}"
            buffer.writeByte(it)
        }

        val list: List<Any> = MoshiPack().unpack(buffer)

        assertEquals(2, list.size)
    }
}