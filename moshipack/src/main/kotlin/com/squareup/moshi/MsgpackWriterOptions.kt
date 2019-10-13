package com.squareup.moshi

import okio.BufferedSink

private val FIX_WRITER: BufferedSink.(byte: Byte, value: Number) -> Unit = { byte, value ->
    writeByte(value.toInt())
}

private val BYTE_WRITER: BufferedSink.(byte: Byte, value: Number) -> Unit = { byte, value ->
    writeByte(byte.toInt())
    writeByte(value.toInt())
}

private val SHORT_WRITER: BufferedSink.(byte: Byte, value: Number) -> Unit = { byte, value ->
    writeByte(byte.toInt())
    writeShort(value.toInt())
}

private val INT_WRITER: BufferedSink.(byte: Byte, value: Number) -> Unit = { byte, value ->
    writeByte(byte.toInt())
    writeInt(value.toInt())
}

private val LONG_WRITER: BufferedSink.(byte: Byte, value: Number) -> Unit = { byte, value ->
    writeByte(byte.toInt())
    writeLong(value.toLong())
}

enum class MsgpackIntByte(val byte: Byte,
                          val writer: BufferedSink.(byte: Byte, value: Number) -> Unit) {
    FIX_INT(0x0, FIX_WRITER),
    INT_8(MsgpackFormat.INT_8, BYTE_WRITER),
    INT_16(MsgpackFormat.INT_16, SHORT_WRITER),
    INT_32(MsgpackFormat.INT_32, INT_WRITER),
    INT_64(MsgpackFormat.INT_64, LONG_WRITER),
    UINT_8(MsgpackFormat.UINT_8, BYTE_WRITER),
    UINT_16(MsgpackFormat.UINT_16, SHORT_WRITER),
    UINT_32(MsgpackFormat.UINT_32, INT_WRITER),
    UINT_64(MsgpackFormat.UINT_64, LONG_WRITER)
}

enum class MsgpackFloatByte(val byte: Byte) {
    FLOAT_32(MsgpackFormat.FLOAT_32),
    FLOAT_64(MsgpackFormat.FLOAT_64)
}

class MsgpackWriterOptions(
        var writeAllIntsAs: MsgpackIntByte? = null,
        var writeAllFloatsAs: MsgpackFloatByte? = null,
        var writeAllIntsAsFloats: Boolean = false
)
