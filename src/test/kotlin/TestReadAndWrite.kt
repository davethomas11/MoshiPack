import junit.framework.Assert.assertEquals
import org.junit.Test

class TestReadAndWrite {

    @Test
    fun writeAndReadSomeNumbers() {

        val numbers = SomeNumbers(255, 55.5F, 3, 500.25)

        val buffer = MoshiPack.pack(numbers)

        val unmarshalled = MoshiPack.unpack<SomeNumbers>(buffer)

        assertEquals(255, unmarshalled.num1)
        assertEquals(55.5F, unmarshalled.num2)
        assertEquals(3, unmarshalled.num3)
        assertEquals(500.25, unmarshalled.num4, 0.0)
    }
}