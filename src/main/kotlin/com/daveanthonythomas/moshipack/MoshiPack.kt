package com.daveanthonythomas.moshipack

import com.squareup.moshi.*
import okio.Buffer
import okio.BufferedSource
import java.lang.reflect.ParameterizedType
import java.lang.reflect.Type

class MoshiPack(private var builder: Moshi.Builder.() -> kotlin.Unit = {},
                var moshi: Moshi = MoshiPack.moshi(builder)) {

    val jsonToMsgpack = FormatInterchange(Format.Json(), Format.Msgpack())
    val msgpackToJson = FormatInterchange(Format.Msgpack(), Format.Json())

    companion object {
        inline fun <reified T> pack(value: T, moshi: Moshi) =
                Buffer().also { moshi.adapter<T>(T::class.java).toJson(MsgpackWriter(it), value) }

        inline fun <reified T> pack(value: T, crossinline builder: Moshi.Builder.() -> Unit = {}) =
                Buffer().also { moshiAdapter<T>(T::class.java, builder).toJson(MsgpackWriter(it), value) }

        inline fun <reified T> unpack(source: BufferedSource, moshi: Moshi) =
                moshi.adapter<T>(T::class.java).fromJson(MsgpackReader(source)) as T

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
                moshi(builder).adapter<T>(type)

        inline fun moshi(crossinline  builder: Moshi.Builder.() -> Unit = {}) =
                Moshi.Builder().apply(builder).build()
    }

    inline fun <reified T> pack(value: T) = MoshiPack.pack(value, moshi)
    inline fun <reified T> packToByteArray(value: T): ByteArray = MoshiPack.pack(value, moshi).readByteArray()
    inline fun <reified T> unpack(bytes: ByteArray): T = unpack(Buffer().apply { write(bytes) })
    inline fun <reified T> unpack(source: BufferedSource) = MoshiPack.unpack<T>(source, moshi)

    fun msgpackToJson(bytes: ByteArray) = msgpackToJson(Buffer().apply { write(bytes) })
    fun msgpackToJson(source: BufferedSource) = msgpackToJson.transform(source).readUtf8()
    fun jsonToMsgpack(jsonString: String) = jsonToMsgpack(Buffer().apply { writeUtf8(jsonString) })
    fun jsonToMsgpack(source: BufferedSource) = jsonToMsgpack.transform(source)
}