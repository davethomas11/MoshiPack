package com.squareup.moshi

import com.squareup.moshi.JsonScope.EMPTY_DOCUMENT
import com.squareup.moshi.JsonScope.NONEMPTY_OBJECT
import okio.BufferedSource
import java.io.IOException

class MsgpackReader(private val source: BufferedSource) : JsonReader() {

    private val PEEKED_NONE = 0
    private val PEEKED_BEGIN_OBJECT = 1
    private val PEEKED_END_OBJECT = 2
    private val PEEKED_BEGIN_ARRAY = 3
    private val PEEKED_END_ARRAY = 4
    private val PEEKED_TRUE = 5
    private val PEEKED_FALSE = 6
    private val PEEKED_NULL = 7
    private val PEEKED_STRING = 8
    private val PEEKED_LONG = 16
    private val PEEKED_DOUBLE = 17
    private val PEEKED_EOF = 18
    private val PEEKED_BUFFERED = 19
    private val PEEKED_BUFFERED_NAME = 20

    private var promotedNameToValue = false
    private var peekedString: String = ""
    var pathSize = LongArray(32) { 0 }
    var currentTag: Byte = 0
    private val buffer = source.buffer()
    var peeked = PEEKED_NONE

    init {
        pushScope(EMPTY_DOCUMENT)
    }

    override fun beginArray() {
        val p = peeked
        if (p == PEEKED_NONE) doPeek()
        pushScope(JsonScope.EMPTY_ARRAY)
        peeked = PEEKED_NONE
        pathIndices[stackSize - 1] = 0
        pathSize[stackSize - 1] = MsgpackFormat.ARRAY.typeFor(currentTag)?.readSize(source, currentTag)
                ?: throw IllegalStateException("Current tag 0x${currentTag.toString(16)} is not an array tag.")
    }

    override fun endArray() {
        stackSize--
        pathNames[stackSize] = null // Free the last path name so that it can be garbage collected!
        pathIndices[stackSize - 1]++
    }

    override fun beginObject() {
        val p = peeked
        if (p == PEEKED_NONE) doPeek()
        pushScope(JsonScope.EMPTY_OBJECT)
        peeked = PEEKED_NONE
        pathIndices[stackSize - 1] = 0
        pathSize[stackSize - 1] = MsgpackFormat.MAP.typeFor(currentTag)?.readSize(source, currentTag)
                ?: throw IllegalStateException("Current tag 0x${currentTag.toString(16)} is not a map tag.")
    }

    override fun endObject() {
        stackSize--
        pathNames[stackSize] = null // Free the last path name so that it can be garbage collected!
        pathIndices[stackSize - 1]++
    }

    private fun readString(): String {
        if (peeked == PEEKED_NONE) doPeek()

        val readBytes = MsgpackFormat.STR.typeFor(currentTag)?.readSize(source, currentTag)
                ?: throw IllegalStateException("Current tag 0x${currentTag.toString(16)} is not a string tag.")
        return source.readUtf8(readBytes)
    }

    override fun nextBoolean(): Boolean {
        if (peeked == PEEKED_NONE) doPeek()
        peeked = PEEKED_NONE
        pathIndices[stackSize - 1]++

        return currentTag == MsgpackFormat.TRUE
    }

    override fun <T : Any?> nextNull(): T? {
        if (peeked == PEEKED_NONE) doPeek()
        peeked = PEEKED_NONE
        pathIndices[stackSize - 1]++

        return null
    }

    override fun nextDouble(): Double = readNumber().toDouble()

    override fun nextLong(): Long = readNumber().toLong()

    override fun nextInt(): Int = readNumber().toInt()

    private fun readNumber(): Number {
        val p = peeked
        if (p == PEEKED_NONE) doPeek()

        pathIndices[stackSize - 1]++
        peeked = PEEKED_NONE

        return when (currentTag) {
            in MsgpackFormat.STR -> {
                val readBytes = MsgpackFormat.STR.typeFor(currentTag)?.readSize(source, currentTag)
                if (readBytes != null) {
                    source.readUtf8(readBytes).toDouble()
                } else {
                    throw AssertionError()
                }
            }
            MsgpackFormat.UINT_8 -> source.readByte().toInt() and 0xff
            MsgpackFormat.UINT_16 -> source.readShort().toInt() and 0xffff
            MsgpackFormat.UINT_32 -> source.readInt().toLong() and 0xffffffff
            MsgpackFormat.UINT_64 -> source.readLong()
            in MsgpackFormat.FIX_INT_MIN..MsgpackFormat.FIX_INT_MAX -> currentTag
            MsgpackFormat.FLOAT_64 -> Double.fromBits(source.readLong())
            MsgpackFormat.FLOAT_32 -> java.lang.Float.intBitsToFloat(source.readInt())
            MsgpackFormat.INT_8 -> source.readByte()
            MsgpackFormat.INT_32 -> source.readInt()
            MsgpackFormat.INT_16 -> source.readShort()
            MsgpackFormat.INT_64 -> source.readLong()
            else -> throw IllegalStateException("Current tag 0x${currentTag.toString(16)} is not a supported number tag.")
        }
    }

    override fun close() {
        peeked = PEEKED_NONE
        scopes[0] = JsonScope.CLOSED
        stackSize = 1
        buffer.clear()
        source.close()
    }

    override fun hasNext(): Boolean {
        val p = peeked
        if (p == PEEKED_NONE) doPeek()
        return pathIndices[stackSize - 1] < pathSize[stackSize - 1]
    }

    @Throws(IOException::class)
    override fun nextName(): String {
        var p = peeked
        if (p == PEEKED_NONE) {
            p = doPeek()
        }
        val result: String
        if (p == PEEKED_STRING) {
            result = readString()
        } else if (p == PEEKED_BUFFERED_NAME) {
            result = peekedString
        } else {
            throw JsonDataException("Expected a name but was " + peek() + " at path " + path)
        }
        peeked = PEEKED_NONE
        pathNames[stackSize - 1] = result
        return result
    }

    @Throws(IOException::class)
    override fun selectName(options: JsonReader.Options): Int {
        var p = peeked
        if (p == PEEKED_NONE) {
            p = doPeek()
        }
        if (p != PEEKED_STRING && p != PEEKED_BUFFERED_NAME) {
            return -1
        }
        if (p == PEEKED_BUFFERED_NAME) {
            return findName(peekedString, options)
        }

        // Save the last recorded path name, so that we
        // can restore the peek state in case we fail to find a match.
        val lastPathName = pathNames[stackSize - 1]

        val nextName = nextName()
        val result = findName(nextName, options)

        if (result == -1) {
            peeked = PEEKED_BUFFERED_NAME
            peekedString = nextName
            // We can't push the path further, make it seem like nothing happened.
            pathNames[stackSize - 1] = lastPathName
        }

        return result
    }

    /**
     * If `name` is in `options` this consumes it and returns it's index.
     * Otherwise this returns -1 and no name is consumed.
     */
    private fun findName(name: String, options: JsonReader.Options): Int {
        var i = 0
        val size = options.strings.size
        while (i < size) {
            if (name == options.strings[i]) {
                peeked = PEEKED_NONE
                pathNames[stackSize - 1] = name

                return i
            }
            i++
        }
        return -1
    }

    @Throws(IOException::class)
    override fun nextString(): String {
        var p = peeked
        if (p == PEEKED_NONE) {
            p = doPeek()
        }
        val result: String
        if (p == PEEKED_STRING) {
            result = readString()
        } else if (p == PEEKED_BUFFERED) {
            result = peekedString
            peekedString = ""
        } else if (p == PEEKED_LONG) {
            result = nextDouble().toString()
        } else {
            throw JsonDataException("Expected a string but was " + peek() + " at path " + path)
        }
        peeked = PEEKED_NONE
        if (!promotedNameToValue) pathIndices[stackSize - 1]++
        else promotedNameToValue = false
        return result
    }

    @Throws(IOException::class)
    override fun selectString(options: JsonReader.Options): Int {
        var p = peeked
        if (p == PEEKED_NONE) {
            p = doPeek()
        }
        if (p != PEEKED_STRING && p != PEEKED_BUFFERED) {
            return -1
        }
        if (p == PEEKED_BUFFERED) {
            return findString(peekedString, options)
        }

        val nextString = nextString()
        val result = findString(nextString, options)

        if (result == -1) {
            peeked = PEEKED_BUFFERED
            peekedString = nextString
            pathIndices[stackSize - 1]--
        }

        return result
    }

    /**
     * If `string` is in `options` this consumes it and returns it's index.
     * Otherwise this returns -1 and no string is consumed.
     */
    private fun findString(string: String, options: JsonReader.Options): Int {
        var i = 0
        val size = options.strings.size
        while (i < size) {
            if (string == options.strings[i]) {
                peeked = PEEKED_NONE
                pathIndices[stackSize - 1]++

                return i
            }
            i++
        }
        return -1
    }

    override fun promoteNameToValue() {
        if (hasNext()) {
            peekedString = nextName()
            peeked = PEEKED_BUFFERED
            promotedNameToValue = true
        }
    }

    override fun skipValue() {
        if (peeked == PEEKED_NONE) doPeek()
        when (peek()) {
            JsonReader.Token.BEGIN_ARRAY -> skipArray()
            JsonReader.Token.BEGIN_OBJECT -> skipObject()
            JsonReader.Token.STRING -> nextString()
            JsonReader.Token.NUMBER -> readNumber()
            JsonReader.Token.BOOLEAN -> nextBoolean()
            JsonReader.Token.NULL -> {
                peeked = PEEKED_NONE
                pathIndices[stackSize - 1]++
            }
            else -> return
        }
    }

    private fun skipObject() {
        beginObject()
        while(hasNext()) {
            nextName()
            skipValue()
        }
        endObject()
    }

    private fun skipArray() {
        beginArray()
        while(hasNext()) {
            skipValue()
        }
        endArray()
    }

    override fun peek(): Token {
        var p = peeked
        if (p == PEEKED_NONE) {
            doPeek()
            p = peeked
        }

        return when (p) {
            PEEKED_BEGIN_OBJECT -> Token.BEGIN_OBJECT
            PEEKED_END_OBJECT -> Token.END_OBJECT
            PEEKED_BEGIN_ARRAY -> Token.BEGIN_ARRAY
            PEEKED_END_ARRAY -> Token.END_ARRAY
            PEEKED_STRING, PEEKED_BUFFERED -> Token.STRING
            PEEKED_TRUE, PEEKED_FALSE -> Token.BOOLEAN
            PEEKED_NULL -> Token.NULL
            PEEKED_DOUBLE, PEEKED_LONG -> Token.NUMBER
            else -> throw AssertionError()
        }
    }

    @Throws(IOException::class)
    private fun doPeek(): Int {
        val peekStack = scopes[stackSize - 1]
        if (peekStack == JsonScope.EMPTY_ARRAY) {
            scopes[stackSize - 1] = JsonScope.NONEMPTY_ARRAY
        } else if (peekStack == JsonScope.EMPTY_OBJECT || peekStack == JsonScope.NONEMPTY_OBJECT) {
            scopes[stackSize - 1] = JsonScope.DANGLING_NAME
        } else if (peekStack == JsonScope.DANGLING_NAME) {
            scopes[stackSize - 1] = JsonScope.NONEMPTY_OBJECT
        } else if (peekStack == JsonScope.EMPTY_DOCUMENT) {
            scopes[stackSize - 1] = JsonScope.NONEMPTY_DOCUMENT
        } else if (peekStack == JsonScope.CLOSED) {
            throw IllegalStateException("JsonReader is closed")
        }

        if (buffer.size() == 0L) {
            peeked = PEEKED_EOF
            return peeked
        }

        val c = buffer.readByte()
        when (c) {
            in MsgpackFormat.ARRAY -> peeked = PEEKED_BEGIN_ARRAY
            in MsgpackFormat.MAP -> peeked = PEEKED_BEGIN_OBJECT
            in MsgpackFormat.STR -> peeked = PEEKED_STRING
            in MsgpackFormat.FIX_INT_MIN..MsgpackFormat.FIX_INT_MAX,
            MsgpackFormat.UINT_8,
            MsgpackFormat.UINT_16,
            MsgpackFormat.UINT_32,
            MsgpackFormat.UINT_64,
            MsgpackFormat.INT_8,
            MsgpackFormat.INT_16,
            MsgpackFormat.INT_32,
            MsgpackFormat.INT_64 -> peeked = PEEKED_LONG
            MsgpackFormat.FLOAT_32,
            MsgpackFormat.FLOAT_64 -> peeked = PEEKED_DOUBLE
            MsgpackFormat.NIL -> peeked = PEEKED_NULL
            MsgpackFormat.TRUE -> peeked = PEEKED_TRUE
            MsgpackFormat.FALSE -> peeked = PEEKED_FALSE
            else -> throw IllegalStateException("Msgpack format tag not yet supported: 0x${String.format("%02X", c)}")
        }

        currentTag = c
        return peeked
    }
}
