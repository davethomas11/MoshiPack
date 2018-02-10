package com.squareup.moshi

import okio.BufferedSink
import okio.BufferedSource

object MsgpackFormat {

    const val FIX_INT_MAX = 0x7f

    const val SIZE_8 = (2e8 - 1).toLong()
    const val SIZE_16 = (2e16 - 1).toLong()
    const val SIZE_32 = (2e32 - 1).toLong()

    const val NIL = 0xc0.toByte()
    const val FALSE = 0xc2.toByte()
    const val TRUE = 0xc3.toByte()

    const val FLOAT_32 = 0xca.toByte()
    const val FLOAT_64 = 0xcb.toByte()

    const val INT_16 = 0xd1.toByte()
    const val INT_32 = 0xd2.toByte()
    const val INT_64 = 0xd3.toByte()

    private val FIX_STR = MsgpackFormatType(0xa0.toByte(), 31, isFix = true)
    //private val STR_8 = com.squareup.moshi.MsgpackFormatType(0xd9, SIZE_8)
    private val STR_16 = MsgpackFormatType(0xda.toByte(), SIZE_16)
    private val STR_32 = MsgpackFormatType(0xdb.toByte(), SIZE_32)
    val STR = arrayOf(FIX_STR, STR_16, STR_32)

    private val FIX_ARRAY = MsgpackFormatType(0x90.toByte(), 15, isFix = true)
    private val ARRAY_16 = MsgpackFormatType(0xdc.toByte(), SIZE_16)
    private val ARRAY_32 = MsgpackFormatType(0xdd.toByte(), SIZE_32)
    val ARRAY = arrayOf(FIX_ARRAY, ARRAY_16, ARRAY_32)

    private val FIX_MAP = MsgpackFormatType(0x80.toByte(), 15, isFix = true)
    private val MAP_16 = MsgpackFormatType(0xde.toByte(), SIZE_16)
    private val MAP_32 = MsgpackFormatType(0xdf.toByte(), SIZE_32)
    val MAP = arrayOf(FIX_MAP, MAP_16, MAP_32)

    fun tagFor(type: Array<MsgpackFormatType>, size: Int) =
            type.filter {
                it.maxSize > size
            }.minBy {
                it.maxSize
            }
}

operator fun Array<MsgpackFormatType>.contains(value: Byte) = this.any { value in it }
fun Array<MsgpackFormatType>.typeFor(value: Byte) = this.firstOrNull { value in it }

data class MsgpackFormatType(val tag: Byte, val maxSize: Long, val isFix: Boolean = false) {

    operator fun contains(value: Byte) = if (isFix) value in tag..tag+maxSize else value == tag

    fun writeTag(sink: BufferedSink, size: Int) {
        sink.writeByte(tag + if (isFix) size else 0)
        if (!isFix) writeSize(sink, size)
    }

    fun readSize(source: BufferedSource, value: Byte): Long = if (isFix) (value - tag).toLong() else when (maxSize) {
        MsgpackFormat.SIZE_16 -> source.readShort().toLong()
        MsgpackFormat.SIZE_32 -> source.readInt().toLong()
        else -> throw IllegalStateException("Unable to read size for tag type: 0x" + value.toString(16))
    }

    private fun writeSize(sink: BufferedSink, size: Int) {
        when (maxSize) {
            //TODO com.squareup.moshi.MsgpackFormat.SIZE_8 -> sink.writeShort(size)

            // Need to make sure these are unsigned ints as well
            // Not sure they are written in unsigned format or not
            MsgpackFormat.SIZE_16 -> sink.writeShort(size)
            MsgpackFormat.SIZE_32 -> sink.writeInt(size)
        }
    }
}