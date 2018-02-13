# MoshiPack 

[![CircleCI](https://circleci.com/gh/davethomas11/MoshiPack/tree/master.svg?style=svg)](https://circleci.com/gh/davethomas11/MoshiPack/tree/master) [![Release](https://jitpack.io/v/davethomas11/MoshiPack.svg)](https://jitpack.io/#davethomas11/MoshiPack)

Just getting started here.
Inspired by https://twitter.com/kaushikgopal/status/961426258818039808

----

## Status -> In development ( Not ready ) ```Baking```
Currently finalizing first draft of Reader & Writer. 


## Convert an object to MessagePack format

```
data class MessagePackWebsitePlug(var compact: Boolean = true, var schema: Int = 0)

val moshiPack = MoshiPack()
val packed: BufferedSource = moshiPack.pack(MessagePackWebsitePlug())

println(packed.readByteString().hex())
\\ Prints the MessagePack bytes as a hex string 82a7636f6d70616374c3a6736368656d6100
\\ 82 - Map with two entries

\\ a7 - String of seven bytes 
\\ 63 6f 6d 70 61 63 74 - UTF8 String "compact"
\\ c3 - Boolean value true

\\ a6 - String of size bytes
\\ 73 63 68 65 6d 61 - UTF8 String "schema"
\\ 00 - Integer value 0
```
