import com.daveanthonythomas.moshipack.MoshiPack
import org.junit.Test

class TestCatData {

    @Test
    fun testCatData() {
        val fileContent = TestCatData::class.java.getResource("cats").readBytes()

        val cats: List<Cat> = MoshiPack.unpack(fileContent)
    }
}