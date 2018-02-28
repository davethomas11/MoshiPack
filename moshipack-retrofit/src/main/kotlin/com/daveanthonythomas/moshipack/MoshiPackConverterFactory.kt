package com.daveanthonythomas.moshipack

import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.MsgpackReader
import com.squareup.moshi.MsgpackWriter
import okhttp3.MediaType
import okhttp3.RequestBody
import okhttp3.ResponseBody
import okio.Buffer
import retrofit2.Converter
import retrofit2.Retrofit
import java.lang.reflect.Type
import java.util.Collections.emptySet
import java.util.Collections.unmodifiableSet
import com.squareup.moshi.JsonQualifier
import java.util.*
import com.sun.xml.internal.ws.streaming.XMLStreamReaderUtil.close
import com.squareup.moshi.JsonDataException
import com.squareup.moshi.JsonReader
import okio.ByteString
import okio.ByteString.decodeHex






class MoshiPackConverterFactory(val moshiPack: MoshiPack = MoshiPack(),
                                val failOnUnknown: Boolean = false,
                                val serializeNull: Boolean = false) : Converter.Factory() {

    companion object {
        val UTF8_BOM = ByteString.decodeHex("EFBBBF")
    }

    override fun responseBodyConverter(type: Type?,
                                       annotations: Array<out Annotation>?,
                                       retrofit: Retrofit?): Converter<ResponseBody, *>? {
        return if (type != null && annotations != null) {
            var adapter = moshiPack.moshi.adapter<Any>(type, jsonAnnotations(annotations))
            if (failOnUnknown) adapter = adapter.failOnUnknown()
            if (serializeNull) adapter = adapter.serializeNulls()
            return MoshiPackResponseBodyConverter(adapter)
        } else null
    }

    override fun requestBodyConverter(type: Type?,
                                      parameterAnnotations: Array<out Annotation>?,
                                      methodAnnotations: Array<out Annotation>?,
                                      retrofit: Retrofit?): Converter<*, RequestBody>? {
        return if (type != null && parameterAnnotations != null) {
            var adapter = moshiPack.moshi.adapter<Any>(type, jsonAnnotations(parameterAnnotations))
            if (failOnUnknown) adapter = adapter.failOnUnknown()
            if (serializeNull) adapter = adapter.serializeNulls()
            return MoshiPackRequestBodyConverter(adapter)
        } else null
    }

    private class MoshiPackResponseBodyConverter<T>(private val adapter: JsonAdapter<T>) : Converter<ResponseBody, T> {

        override fun convert(value: ResponseBody?): T? = if (value != null) {
            val source = value.source()
            value.use {
                // Moshi has no document-level API so the responsibility of BOM skipping falls to whatever
                // is delegating to it. Since it's a UTF-8-only library as well we only honor the UTF-8 BOM.
                if (source.rangeEquals(0, UTF8_BOM)) {
                    source.skip(UTF8_BOM.size().toLong())
                }
                val reader = MsgpackReader(source)
                adapter.fromJson(reader)
            }
        } else {
            null
        }
    }

    private class MoshiPackRequestBodyConverter<T>(private val adapter: JsonAdapter<T>) : Converter<T, RequestBody> {
        private val MEDIA_TYPE = MediaType.parse("application/x-msgpack; charset=utf-8")

        override fun convert(value: T): RequestBody {
            val buffer = Buffer()
            adapter.toJson(MsgpackWriter(buffer), value)
            return RequestBody.create(MEDIA_TYPE, buffer.readByteString())
        }
    }

    private fun jsonAnnotations(annotations: Array<out Annotation>): Set<Annotation> {
        var result: MutableSet<Annotation>? = null
        for (annotation in annotations) {
            if (annotation.annotationClass.java.isAnnotationPresent(JsonQualifier::class.java)) {
                if (result == null) result = LinkedHashSet()
                result.add(annotation)
            }
        }
        return if (result != null) unmodifiableSet(result) else emptySet()
    }
}