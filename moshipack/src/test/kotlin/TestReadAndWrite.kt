import com.daveanthonythomas.moshipack.MoshiPack
import org.junit.Assert.assertEquals
import org.junit.Test

class TestReadAndWrite {

    @Test
    fun writeAndReadSomeNumbers() {

        val numbers = SomeNumbers(255, 55.5F, 3, 500.25, 1536094776000L)

        val buffer = MoshiPack.pack(numbers)

        val unmarshalled = MoshiPack.unpack<SomeNumbers>(buffer)

        assertEquals(255, unmarshalled.num1)
        assertEquals(55.5F, unmarshalled.num2)
        assertEquals(3.toShort(), unmarshalled.num3)
        assertEquals(500.25, unmarshalled.num4, 0.0)
        assertEquals(1536094776000L, unmarshalled.num5)
    }

    @Test
    fun writeAndReadSomeNegativeNumbers() {

        val numbers = SomeNumbers(-255, -55.5F, -3, -500.25, -1536094776123L)

        val buffer = MoshiPack.pack(numbers)

        val unmarshalled = MoshiPack.unpack<SomeNumbers>(buffer)

        assertEquals(-255, unmarshalled.num1)
        assertEquals(-55.5F, unmarshalled.num2)
        assertEquals((-3).toShort(), unmarshalled.num3)
        assertEquals(-500.25, unmarshalled.num4, 0.0)
        assertEquals(-1536094776123L, unmarshalled.num5)
    }

    @Test
    fun nestReadAndWrite() {
        val nest = Nest(listOf(Egg(2), Egg(9)))
        val moshiPack = MoshiPack()
        val byteArray = moshiPack.packToByteArray(nest)
        val unpackedNest: Nest = moshiPack.unpack(byteArray)

        assertEquals(nest, unpackedNest)
    }
}
