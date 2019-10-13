package com.daveanthonythomas.moshipack

import com.squareup.moshi.*
import okio.Buffer
import okio.BufferedSource
import java.lang.reflect.Type

class MoshiPack(private var builder: Moshi.Builder.() -> kotlin.Unit = {},
                var moshi: Moshi = MoshiPack.moshi(builder),
                var writerOptions: MsgpackWriterOptions = MsgpackWriterOptions()) {

    private val mpToJson by lazy {
        FormatInterchange(Format.Msgpack(), Format.Json())
    }

    private val jsonToMp by lazy {
        FormatInterchange(Format.Json(), Format.Msgpack())
    }

    companion object {
        inline fun <reified T> pack(value: T, moshiPack: MoshiPack): BufferedSource =
                Buffer().also {
                    moshiPack.moshi.adapter<T>(T::class.java)
                            .toJson(MsgpackWriter(it).apply { options = moshiPack.writerOptions }, value)
                }

        inline fun <reified T> pack(value: T,
                                    moshi: Moshi,
                                    writerOptions: MsgpackWriterOptions = MsgpackWriterOptions()): BufferedSource =
                Buffer().also {
                    moshi.adapter<T>(T::class.java)
                        .toJson(MsgpackWriter(it).apply { options = writerOptions }, value)
                }

        inline fun <reified T> pack(value: T,
                                    crossinline builder: Moshi.Builder.() -> Unit = {},
                                    writerOptions: MsgpackWriterOptions = MsgpackWriterOptions()): BufferedSource =
                pack(value, moshi(builder), writerOptions)

        inline fun <reified T> unpack(source: BufferedSource, moshi: Moshi): T {
            val type: Type = object : TypeReference<T>() {}.type
            return moshi.adapter<T>(type).fromJson(MsgpackReader(source)) as T
        }

        inline fun <reified T> unpack(source: BufferedSource, crossinline builder: Moshi.Builder.() -> Unit = {}): T =
                unpack(source, moshi(builder))

        inline fun <reified T> unpack(bytes: ByteArray, moshi: Moshi): T =
                unpack(Buffer().apply { write(bytes) }, moshi)

        inline fun <reified T> unpack(bytes: ByteArray, crossinline builder: Moshi.Builder.() -> Unit = {}): T =
                unpack(Buffer().apply { write(bytes) }, builder)

        inline fun moshi(crossinline builder: Moshi.Builder.() -> Unit = {}) =
                Moshi.Builder().apply(builder).add(KotlinJsonAdapterFactory()).build()

        fun msgpackToJson(bytes: ByteArray,
                          writerOptions: MsgpackWriterOptions = MsgpackWriterOptions()): String =
                msgpackToJson(Buffer().apply { write(bytes) }, writerOptions)

        fun msgpackToJson(source: BufferedSource,
                          writerOptions: MsgpackWriterOptions = MsgpackWriterOptions()) =
                FormatInterchange(Format.Msgpack(writerOptions), Format.Json())
                        .transform(source).readUtf8()

        fun jsonToMsgpack(jsonString: String,
                          writerOptions: MsgpackWriterOptions = MsgpackWriterOptions()) =
                jsonToMsgpack(Buffer().apply { writeUtf8(jsonString) })

        fun jsonToMsgpack(source: BufferedSource,
                          writerOptions: MsgpackWriterOptions = MsgpackWriterOptions()): BufferedSource =
                FormatInterchange(Format.Json(), Format.Msgpack(writerOptions))
                        .transform(source)
    }

    inline fun <reified T> pack(value: T) = MoshiPack.pack(value, this)
    inline fun <reified T> packToByteArray(value: T): ByteArray = pack(value).readByteArray()
    inline fun <reified T> unpack(bytes: ByteArray): T = unpack(Buffer().apply { write(bytes) })
    inline fun <reified T> unpack(source: BufferedSource): T = MoshiPack.unpack(source, moshi)

    fun msgpackToJson(bytes: ByteArray): String = msgpackToJson(Buffer().apply { write(bytes) })
    fun msgpackToJson(source: BufferedSource) = mpToJson.transform(source).readUtf8()

    fun jsonToMsgpack(jsonString: String) = jsonToMsgpack(Buffer().apply { writeUtf8(jsonString) })
    fun jsonToMsgpack(source: BufferedSource): BufferedSource = jsonToMp.transform(source)
}
