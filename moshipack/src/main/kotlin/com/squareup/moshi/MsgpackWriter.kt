package com.squareup.moshi

import com.squareup.moshi.JsonScope.DANGLING_NAME
import com.squareup.moshi.JsonScope.EMPTY_ARRAY
import com.squareup.moshi.JsonScope.EMPTY_DOCUMENT
import com.squareup.moshi.JsonScope.EMPTY_OBJECT
import com.squareup.moshi.JsonScope.NONEMPTY_ARRAY
import com.squareup.moshi.JsonScope.NONEMPTY_DOCUMENT
import com.squareup.moshi.JsonScope.NONEMPTY_OBJECT
import okio.Buffer
import okio.BufferedSink
import java.io.IOException

class MsgpackWriter(private val sink: BufferedSink, private val forcedOutputType: Byte? = null) : JsonWriter() {

    private var deferredName: String? = null
    private val pathBuffers = Array<Buffer?>(32) { null }

    private val currentIndex get() = stackSize - 1
    private val currentBuffer
        get() = if (currentIndex == 0) sink else pathBuffers[currentIndex]
            ?: throw IllegalStateException("Path buffer not initialized.")

    init {
        pushScope(EMPTY_DOCUMENT)
    }

    override fun nullValue(): JsonWriter {
        if (deferredName != null) {
            if (serializeNulls) {
                writeDeferredName()
            } else {
                deferredName = null
                return this
            }
        }
        beforeValue()
        currentBuffer.writeByte(MsgpackFormat.NIL.toInt())
        pathIndices[currentIndex]++
        return this
    }

    override fun value(value: String?): JsonWriter = when {
        value == null -> nullValue()
        promoteValueToName -> name(value)
        else -> {
            writeDeferredName()
            beforeValue()
            string(currentBuffer, value)
            pathIndices[currentIndex]++
            this
        }
    }

    override fun value(value: Boolean): JsonWriter {
        writeDeferredName()
        beforeValue()
        currentBuffer.writeByte(if (value) MsgpackFormat.TRUE.toInt() else MsgpackFormat.FALSE.toInt())
        pathIndices[currentIndex]++
        return this
    }

    override fun value(value: Boolean?): JsonWriter {
        return if (value == null) nullValue() else value(value)
    }

    override fun value(value: Double): JsonWriter {
        if (!lenient && (value.isNaN() || value.isInfinite())) {
            throw IllegalArgumentException("Numeric values must be finite, but was " + value)
        }
        return value(value as Number)
    }

    override fun value(value: Long): JsonWriter {
        if (promoteValueToName) {
            return name(value.toString())
        }
        writeDeferredName()
        beforeValue()
        if (forcedOutputType == null) {
            getSmallestType(value)
        } else {
            getForcedType(value)
        }

        pathIndices[stackSize - 1]++
        return this
    }

    private fun getForcedType(value: Long) {
        when (forcedOutputType) {
            MsgpackFormat.UINT_64 -> {
                currentBuffer.writeByte(MsgpackFormat.UINT_64.toInt())
                currentBuffer.writeLong(value)
            }
            MsgpackFormat.INT_64 -> {
                currentBuffer.writeByte(MsgpackFormat.INT_64.toInt())
                currentBuffer.writeLong(value)
            }
            else -> throw RuntimeException("Target format not supported")
        }
    }

    private fun getSmallestType(value: Long) {
        when (value) {
            in MsgpackFormat.FIX_INT_MIN..MsgpackFormat.FIX_INT_MAX -> {
                currentBuffer.writeByte(value.toInt())
            }
            in MsgpackFormat.UINT_8_MIN..MsgpackFormat.UINT_8_MAX -> {
                currentBuffer.writeByte(MsgpackFormat.UINT_8.toInt())
                currentBuffer.writeByte(value.toInt())
            }
            in MsgpackFormat.UINT_16_MIN..MsgpackFormat.UINT_16_MAX -> {
                currentBuffer.writeByte(MsgpackFormat.UINT_16.toInt())
                currentBuffer.writeShort(value.toInt())
            }
            in MsgpackFormat.UINT_32_MIN..MsgpackFormat.UINT_32_MAX -> {
                currentBuffer.writeByte(MsgpackFormat.UINT_32.toInt())
                currentBuffer.writeInt(value.toInt())
            }
            in MsgpackFormat.UINT_32_MAX+1..Long.MAX_VALUE -> {
                currentBuffer.writeByte(MsgpackFormat.UINT_64.toInt())
                currentBuffer.writeLong(value)
            }
            in Short.MIN_VALUE..Short.MAX_VALUE -> {
                currentBuffer.writeByte(MsgpackFormat.INT_16.toInt())
                currentBuffer.writeShort(value.toInt())
            }
            in Int.MIN_VALUE..Int.MAX_VALUE -> {
                currentBuffer.writeByte(MsgpackFormat.INT_32.toInt())
                currentBuffer.writeInt(value.toInt())
            }
            in Long.MIN_VALUE..Long.MAX_VALUE -> {
                currentBuffer.writeByte(MsgpackFormat.INT_64.toInt())
                currentBuffer.writeLong(value)
            }
        }
    }

    override fun value(value: Number?): JsonWriter {
        if (value == null) return nullValue()

        // If its a whole number store it as an int type
        if (value.toDouble() % 1 == 0.0) {
            return value(value.toLong())
        }

        if (promoteValueToName) {
            return name(value.toString())
        }
        writeDeferredName()
        beforeValue()
        when (value.toDouble()) {
            in -Float.MAX_VALUE..Float.MAX_VALUE -> {
                currentBuffer.writeByte(MsgpackFormat.FLOAT_32.toInt())
                currentBuffer.writeInt(java.lang.Float.floatToIntBits(value.toFloat()))
            }
            in -Double.MAX_VALUE..Double.MAX_VALUE-> {
                currentBuffer.writeByte(MsgpackFormat.FLOAT_64.toInt())
                currentBuffer.writeLong(value.toDouble().toRawBits())
            }
        }

        pathIndices[stackSize - 1]++
        return this
    }

    override fun beginArray(): JsonWriter {
        writeDeferredName()
        return open(EMPTY_ARRAY)
    }

    override fun endArray(): JsonWriter {
        return close(EMPTY_ARRAY, NONEMPTY_ARRAY)
    }

    override fun beginObject(): JsonWriter {
        writeDeferredName()
        return open(EMPTY_OBJECT)
    }

    override fun endObject(): JsonWriter {
        promoteValueToName = false
        return close(EMPTY_OBJECT, NONEMPTY_OBJECT)
    }

    override fun name(name: String?): JsonWriter {
        require(name != null) { "name == null " }
        require(stackSize > 0) { "MsgpackWriter is closed." }
        require(deferredName == null) { "Nesting problem" }
        deferredName = name
        pathNames[currentIndex] = name
        promoteValueToName = false
        return this
    }

    override fun flush() {
        if (stackSize == 0) {
            throw IllegalStateException("JsonWriter is closed.")
        }
        sink.flush()
    }

    override fun close() {
        sink.close()

        val size = stackSize
        if (size > 1 || size == 1 && scopes[size - 1] != NONEMPTY_DOCUMENT) {
            throw IOException("Incomplete document")
        }
        stackSize = 0
    }

    @Throws(IOException::class)
    private fun writeDeferredName() {
        if (deferredName != null) {
            beforeName()
            string(currentBuffer, deferredName ?: throw IllegalStateException("Null name."))
            deferredName = null
        }
    }

    /**
     * Check scope and adjusts the stack to expect the name's value.
     */
    @Throws(IOException::class)
    private fun beforeName() {
        val context = peekScope()
        require(context == EMPTY_OBJECT || context == NONEMPTY_OBJECT) {
            "Nesting problem, not in object."
        }
        replaceTop(DANGLING_NAME)
    }

    /**
     * Inserts any necessary separators and whitespace before a literal value,
     * inline array, or inline object. Also adjusts the stack to expect either a
     * closing bracket or another element.
     */
    @Throws(IOException::class)
    private fun beforeValue() {
        when (peekScope()) {
            NONEMPTY_DOCUMENT,
            EMPTY_DOCUMENT -> replaceTop(NONEMPTY_DOCUMENT)
            EMPTY_ARRAY -> replaceTop(NONEMPTY_ARRAY)
            NONEMPTY_ARRAY -> return
            DANGLING_NAME -> replaceTop(NONEMPTY_OBJECT)
            else -> throw IllegalStateException("Nesting problem.")
        }
    }

    /**
     * Enters a new scope by appending any necessary whitespace and the given
     * bracket.
     */
    @Throws(IOException::class)
    private fun open(empty: Int): JsonWriter {
        beforeValue()
        //checkStack() - Grows stack in newer version of Moshi under development.
        //pathBuffers will have to grow too if this is supported.
        pushScope(empty)
        pathBuffers[currentIndex] = Buffer()
        pathIndices[currentIndex] = 0
        return this
    }

    /**
     * Closes the current scope by appending any necessary whitespace and the
     * given bracket.
     */
    @Throws(IOException::class)
    private fun close(empty: Int, nonempty: Int): JsonWriter {
        val context = peekScope()
        if (context != nonempty && context != empty) {
            throw IllegalStateException("Nesting problem.")
        }
        if (deferredName != null) {
            throw IllegalStateException("Dangling name: " + deferredName)
        }

        val msgType = when (nonempty) {
            NONEMPTY_ARRAY -> MsgpackFormat.ARRAY
            NONEMPTY_OBJECT -> MsgpackFormat.MAP
            else -> throw IllegalStateException("Wrong nonempty value used.")
        }
        val tagType = MsgpackFormat.tagFor(msgType, pathIndices[currentIndex])
                ?: throw IllegalArgumentException("Size too long for msgpack format.")
        val previousBuffer = currentBuffer as Buffer
        val previousSize = pathIndices[currentIndex]

        stackSize--
        tagType.writeTag(currentBuffer, previousSize)
        previousBuffer.copyTo(currentBuffer.outputStream())
        pathNames[stackSize] = null // Free the last path name so that it can be garbage collected!
        pathIndices[currentIndex]++

        if (stackSize == 1) {
            sink.emitCompleteSegments()
        }
        return this
    }

    /**
     * Writes `value` as a string literal to `sink`.
     */
    @Throws(IOException::class)
    private fun string(sink: BufferedSink, value: String) {
        val bytes = value.toByteArray()
        val tagType = MsgpackFormat.tagFor(MsgpackFormat.STR, bytes.size)
                ?: throw IllegalArgumentException("String size too long for msgpack format.")

        tagType.writeTag(sink, bytes.size)
        sink.writeUtf8(value)
    }
}