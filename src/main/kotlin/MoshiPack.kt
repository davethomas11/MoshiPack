import com.squareup.moshi.Moshi
import com.squareup.moshi.MsgpackReader
import com.squareup.moshi.MsgpackWriter
import okio.Buffer
import okio.BufferedSink
import okio.BufferedSource

object MoshiPack {
    inline fun <reified T> pack(value: T, crossinline builder: Moshi.Builder.() -> Unit = {}) =
            Buffer().also {
                Moshi.Builder().apply(builder).build().adapter(T::class.java).toJson(MsgpackWriter(it), value)
            }

    inline fun <reified T> unpack(source: BufferedSource, crossinline builder: Moshi.Builder.() -> Unit = {}) =
            Moshi.Builder().apply(builder).build().adapter(T::class.java).fromJson(MsgpackReader(source))
}