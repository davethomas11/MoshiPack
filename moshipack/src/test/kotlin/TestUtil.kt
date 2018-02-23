import okio.Buffer
import okio.ByteString

operator fun Buffer.plusAssign(string: String) {
    this.write(okio.ByteString.decodeHex(string))
}

val String.hex: String get() = ByteString.encodeUtf8(this).hex()
