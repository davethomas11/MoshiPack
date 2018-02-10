import com.squareup.moshi.Moshi
import com.squareup.moshi.MsgpackReader
import com.squareup.moshi.MsgpackWriter
import com.squareup.moshi.Types
import okio.Buffer
import okio.BufferedSource
import java.lang.reflect.ParameterizedType
import java.lang.reflect.Type

object MoshiPack {

    inline fun <reified T> pack(value: T, crossinline builder: Moshi.Builder.() -> Unit = {}) =
            Buffer().also { moshiAdapter<T>(T::class.java, builder).toJson(MsgpackWriter(it), value) }

    inline fun <reified T> unpack(source: BufferedSource, crossinline builder: Moshi.Builder.() -> Unit = {}) =
            moshiAdapter<T>(T::class.java, builder).fromJson(MsgpackReader(source)) as T

    inline fun <reified T> unpackList(source: BufferedSource, ofClass: Class<*>,
                                      crossinline builder: Moshi.Builder.() -> Unit = {}) =
            moshiAdapter<T>(Types.newParameterizedType(T::class.java, ofClass), builder)
                    .fromJson(MsgpackReader(source)) as T


    inline fun <reified T> unpackList(source: BufferedSource, parameterizedType: ParameterizedType,
                                      crossinline builder: Moshi.Builder.() -> Unit = {}) =
            moshiAdapter<T>(parameterizedType, builder).fromJson(MsgpackReader(source)) as T

    inline fun <reified T> moshiAdapter(type: Type, crossinline builder: Moshi.Builder.() -> Unit = {}) =
            Moshi.Builder().apply(builder).build().adapter<T>(type)

}