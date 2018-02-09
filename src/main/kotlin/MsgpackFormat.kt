import okio.BufferedSink

object MsgpackFormat {

    const val SIZE_8 = (2e8 - 1).toLong()
    const val SIZE_16 = (2e16 - 1).toLong()
    const val SIZE_32 = (2e32 - 1).toLong()

    const val NIL = 0xc0
    const val FALSE = 0xc2
    const val TRUE = 0xc3

    const val FLOAT_32 = 0xca
    const val FLOAT_64 = 0xcb

    const val INT_16 = 0xd1
    const val INT_32 = 0xd2
    const val INT_64 = 0xd3


    private val FIX_STR = MsgpackFormatType(0xa0, 31, isFix = true)
    //private val STR_8 = MsgpackFormatType(0xd9, SIZE_8)
    private val STR_16 = MsgpackFormatType(0xda, SIZE_16)
    private val STR_32 = MsgpackFormatType(0xdb, SIZE_32)
    val STR = arrayOf(FIX_STR, STR_16, STR_32)

    private val FIX_ARRAY = MsgpackFormatType(0x90, 15, isFix = true)
    private val ARRAY_16 = MsgpackFormatType(0xdc, SIZE_16)
    private val ARRAY_32 = MsgpackFormatType(0xdd, SIZE_32)
    val ARRAY = arrayOf(FIX_ARRAY, ARRAY_16, ARRAY_32)

    private val FIX_MAP = MsgpackFormatType(0x80, 15, isFix = true)
    private val MAP_16 = MsgpackFormatType(0xde, SIZE_16)
    private val MAP_32 = MsgpackFormatType(0xdf, SIZE_32)
    val MAP = arrayOf(FIX_MAP, MAP_16, MAP_32)

    fun tagFor(type: Array<MsgpackFormatType>, size: Int) =
            type.filter {
                it.maxSize > size
            }.minBy {
                it.maxSize
            }
}

data class MsgpackFormatType(val tag: Int, val maxSize: Long, val isFix: Boolean = false) {

    fun writeTag(sink: BufferedSink, size: Int) {
        sink.writeByte(tag + if (isFix) size else 0)
        if (!isFix) writeSize(sink, size)
    }

    private fun writeSize(sink: BufferedSink, size: Int) {
        when (maxSize) {
            //TODO MsgpackFormat.SIZE_8 -> sink.writeShort(size)

            // Need to make sure these are unsigned ints as well
            // Not sure they are written in unsigned format or not
            MsgpackFormat.SIZE_16 -> sink.writeShort(size)
            MsgpackFormat.SIZE_32 -> sink.writeInt(size)
        }
    }
}