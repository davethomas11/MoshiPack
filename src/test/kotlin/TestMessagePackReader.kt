import com.squareup.moshi.Moshi
import com.squareup.moshi.MsgpackReader
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

        val pizzas: List<Pizza> = MoshiPack.unpackList(buffer, ofClass = Pizza::class.java)

        assertEquals(3, pizzas.size)

        assertThat(pizzas, hasItems(
                Pizza("pepperoni"),
                Pizza("pepperoni"),
                Pizza("pepperoni")))
    }


}