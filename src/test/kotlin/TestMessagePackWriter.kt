import com.squareup.moshi.Moshi
import com.squareup.moshi.MsgpackWriter
import okio.Buffer
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

}

