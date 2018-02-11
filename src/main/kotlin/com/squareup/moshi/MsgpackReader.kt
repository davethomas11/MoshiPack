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

    var pathSize = LongArray(32) { 0 }
    var currentTag: Byte = 0
    var expectName = false
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

    override fun nextString(): String {
        if (peeked == PEEKED_NONE) doPeek()

        if (expectName) {
            expectName = false
        } else {
            pathIndices[stackSize - 1]++
        }

        peeked = PEEKED_NONE

        if (scopes[stackSize - 1] == NONEMPTY_OBJECT && pathIndices[stackSize - 1] < pathSize[stackSize - 1]) {
            expectName = true
        }

        val readBytes = MsgpackFormat.STR.typeFor(currentTag)?.readSize(source, currentTag)
                ?: throw IllegalStateException("Current tag 0x${currentTag.toString(16)} is not a string tag.")
        return source.readUtf8(readBytes)
    }

    override fun nextName() = nextString()

    override fun nextBoolean(): Boolean {
        if (peeked == PEEKED_NONE) doPeek()
        peeked = PEEKED_NONE
        pathIndices[stackSize - 1]++

        if (scopes[stackSize - 1] == NONEMPTY_OBJECT && pathIndices[stackSize - 1] < pathSize[stackSize - 1]) {
            expectName = true
        }

        return currentTag == MsgpackFormat.TRUE
    }

    override fun <T : Any?> nextNull(): T? {
        if (peeked == PEEKED_NONE) doPeek()
        peeked = PEEKED_NONE
        pathIndices[stackSize - 1]++

        if (scopes[stackSize - 1] == NONEMPTY_OBJECT && pathIndices[stackSize - 1] < pathSize[stackSize - 1]) {
            expectName = true
        }

        return null
    }

    override fun nextDouble(): Double = readNumber().toDouble()

    override fun nextLong(): Long  = readNumber().toLong()

    override fun nextInt(): Int = readNumber().toInt()

    private fun readNumber(): Number {
        val p = peeked
        if (p == PEEKED_NONE) doPeek()



        pathIndices[stackSize - 1]++
        peeked = PEEKED_NONE
        if (scopes[stackSize - 1] == NONEMPTY_OBJECT && pathIndices[stackSize - 1] < pathSize[stackSize - 1]) {
            expectName = true
        }
        return when (currentTag) {
            in MsgpackFormat.STR -> {
                val readBytes = MsgpackFormat.STR.typeFor(currentTag)?.readSize(source, currentTag)
                if (readBytes != null) {
                    source.readUtf8(readBytes).toDouble()
                } else {
                    throw AssertionError()
                }
            }
            in 0..MsgpackFormat.FIX_INT_MAX -> currentTag
            MsgpackFormat.FLOAT_64 -> Double.fromBits(source.readLong())
            MsgpackFormat.FLOAT_32 -> java.lang.Float.intBitsToFloat(source.readInt())
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

    override fun selectName(options: Options?): Int {
        val name = nextName()
        return pathIndices[stackSize - 1]
        //TODO "Need proper skipping and indexing here"
    }

    override fun selectString(options: Options?): Int {
        TODO("Not sure what to do here yet")
    }

    override fun promoteNameToValue() {
    }

    override fun skipValue() {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun peek(): Token {
        var p = peeked
        if (p == PEEKED_NONE){
            doPeek()
            p = peeked
        }

        return when (p) {
            PEEKED_BEGIN_OBJECT -> Token.BEGIN_OBJECT
            PEEKED_END_OBJECT -> Token.END_OBJECT
            PEEKED_BEGIN_ARRAY -> Token.BEGIN_ARRAY
            PEEKED_END_ARRAY -> Token.END_ARRAY
            PEEKED_STRING -> Token.STRING
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
            in MsgpackFormat.ARRAY -> {
                peeked = PEEKED_BEGIN_ARRAY
                expectName = true
            }
            in MsgpackFormat.MAP -> {
                peeked = PEEKED_BEGIN_OBJECT
                expectName = true
            }
            in MsgpackFormat.STR -> peeked = PEEKED_STRING
            in 0..MsgpackFormat.FIX_INT_MAX,
            MsgpackFormat.INT_16,
            MsgpackFormat.INT_32,
            MsgpackFormat.INT_64 -> peeked = PEEKED_LONG
            MsgpackFormat.FLOAT_32,
            MsgpackFormat.FLOAT_64 ->  peeked = PEEKED_DOUBLE
            MsgpackFormat.NIL -> peeked = PEEKED_NULL
            MsgpackFormat.TRUE -> peeked = PEEKED_TRUE
            MsgpackFormat.FALSE -> peeked = PEEKED_FALSE
            else -> throw IllegalStateException("Msgpack format tag not yet supported: 0x" + c.toString(16))
        }

        currentTag = c
        return peeked
    }
}