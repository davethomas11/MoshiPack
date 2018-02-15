import com.daveanthonythomas.moshipack.MoshiPack
import okio.ByteString
import org.junit.Assert.assertEquals
import org.junit.Test

class TestTransform {

    @Test
    fun fromJsonToMsgpack() {
        val pack = MoshiPack.jsonToMsgpack("{\"compact\":true,\"schema\":0}")
        assertEquals("82a7${"compact".hex}c3a6${"schema".hex}00", pack.readByteString().hex())
    }

    @Test
    fun fromMsgpackToJson() {
        val json = MoshiPack.msgpackToJson(ByteString.decodeHex("82a7${"compact".hex}c3a6${"schema".hex}00").toByteArray())
        assertEquals("{\"compact\":true,\"schema\":0.0}", json)
    }
}