MoshiPack 
=========

[![CircleCI](https://circleci.com/gh/davethomas11/MoshiPack/tree/master.svg?style=svg)](https://circleci.com/gh/davethomas11/MoshiPack/tree/master) 

### Gradle


```
implementation com.daveanthonythomas.moshipack:moshipack:1.0.1
```

Optional Retrofit support:
```
implementation com.daveanthonythomas.moshipack:moshipack-retrofit:1.0.1
```

## About

This is a Kotilin implementation of MessagePack serialization and deserialization built ontop of Moshi to take advantage of Moshi's type adapters and utilizes okio for reading and writing MessagePack bytes.

The library is intended to be consumed in a Kotlin project, and is not intended for Java use.

Inspired by Kaushik Gopal's [tweet](https://twitter.com/kaushikgopal/status/961426258818039808)

See [Moshi](https://github.com/square/moshi) for adapter usage and reference.

### Convert an object to [MessagePack](https://msgpack.org) format

```kotlin
data class MessagePackWebsitePlug(var compact: Boolean = true, var schema: Int = 0)

val moshiPack = MoshiPack()
val packed: BufferedSource = moshiPack.pack(MessagePackWebsitePlug())

println(packed.readByteString().hex())
```
This prints the MessagePack bytes as a hex string **82a7636f6d70616374c3a6736368656d6100**

- **82** - Map with two entries
- **a7** - String of seven bytes 
- **63 6f 6d 70 61 63 74** - UTF8 String "compact"
- **c3** - Boolean value true
- **a6** - String of size bytes
- **73 63 68 65 6d 61** - UTF8 String "schema"
- **00** - Integer value 0


### Convert binary MessagePack back to an Object
```kotlin
val bytes = ByteString.decodeHex("82a7636f6d70616374c3a6736368656d6100").toByteArray()

val moshiPack = MoshiPack()
val plug: MessagePackWebsitePlug = moshiPack.unpack(bytes)
```

### Static API

If you prefer to not instantiate a ```MoshiPack``` instance you can access the API in a static fashion as well. Note this will create a new ```Moshi``` instance every time you make an API call. You may want to use the API this way if you aren't providing ```MoshiPack``` by some form of dependency injection and you do not have any specific builder parameters for ```Moshi```

---

## Format Support

See [MessagePack format spec](https://github.com/msgpack/msgpack/blob/master/spec.md) for further reference.
<table>
  <tr><th>format name</th><th>first byte (in binary)</th><th>first byte (in hex)</th><th>Supported</th></tr>
  <tr><td>positive fixint</td><td>0xxxxxxx</td><td>0x00 - 0x7f</td><td>Yes</font></td></tr>
  <tr><td>fixmap</td><td>1000xxxx</td><td>0x80 - 0x8f</td><td>Yes</td></tr>
  <tr><td>fixarray</td><td>1001xxxx</td><td>0x90 - 0x9f</td><td>Yes</td></tr>
  <tr><td>fixstr</td><td>101xxxxx</td><td>0xa0 - 0xbf</td><td>Yes</td></tr>
  <tr><td>nil</td><td>11000000</td><td>0xc0</td><td>Yes</td></tr>
  <tr><td>(never used)</td><td>11000001</td><td>0xc1</td><td>Yes</td></tr>
  <tr><td>false</td><td>11000010</td><td>0xc2</td><td>Yes</td></tr>
  <tr><td>true</td><td>11000011</td><td>0xc3</td><td>Yes</td></tr>
  <tr><td>bin 8</td><td>11000100</td><td>0xc4</td><td>No</td></tr>
  <tr><td>bin 16</td><td>11000101</td><td>0xc5</td><td>No</td></tr>
  <tr><td>bin 32</td><td>11000110</td><td>0xc6</td><td>No</td></tr>
  <tr><td>ext 8</td><td>11000111</td><td>0xc7</td><td>No</td></tr>
  <tr><td>ext 16</td><td>11001000</td><td>0xc8</td><td>No</td></tr>
  <tr><td>ext 32</td><td>11001001</td><td>0xc9</td><td>No</td></tr>
  <tr><td>float 32</td><td>11001010</td><td>0xca</td><td>Yes</td></tr>
  <tr><td>float 64</td><td>11001011</td><td>0xcb</td><td>Yes</td></tr>
  <tr><td>uint 8</td><td>11001100</td><td>0xcc</td><td>Yes</td></tr>
  <tr><td>uint 16</td><td>11001101</td><td>0xcd</td><td>Yes</td></tr>
  <tr><td>uint 32</td><td>11001110</td><td>0xce</td><td>Yes</td></tr>
  <tr><td>uint 64</td><td>11001111</td><td>0xcf</td><td>Yes</td></tr>
  <tr><td>int 8</td><td>11010000</td><td>0xd0</td><td>Yes</td></tr>
  <tr><td>int 16</td><td>11010001</td><td>0xd1</td><td>Yes</td></tr>
  <tr><td>int 32</td><td>11010010</td><td>0xd2</td><td>Yes</td></tr>
  <tr><td>int 64</td><td>11010011</td><td>0xd3</td><td>Yes</td></tr>
  <tr><td>fixext 1</td><td>11010100</td><td>0xd4</td><td>No</td></tr>
  <tr><td>fixext 2</td><td>11010101</td><td>0xd5</td><td>No</td></tr>
  <tr><td>fixext 4</td><td>11010110</td><td>0xd6</td><td>No</td></tr>
  <tr><td>fixext 8</td><td>11010111</td><td>0xd7</td><td>No</td></tr>
  <tr><td>fixext 16</td><td>11011000</td><td>0xd8</td><td>No</td></tr>
  <tr><td>str 8</td><td>11011001</td><td>0xd9</td><td>Yes</td></tr>
  <tr><td>str 16</td><td>11011010</td><td>0xda</td><td>Yes</td></tr>
  <tr><td>str 32</td><td>11011011</td><td>0xdb</td><td>Yes</td></tr>
  <tr><td>array 16</td><td>11011100</td><td>0xdc</td><td>Yes</td></tr>
  <tr><td>array 32</td><td>11011101</td><td>0xdd</td><td>Yes</td></tr>
  <tr><td>map 16</td><td>11011110</td><td>0xde</td><td>Yes</td></tr>
  <tr><td>map 32</td><td>11011111</td><td>0xdf</td><td>Yes</td></tr>
  <tr><td>negative fixint</td><td>111xxxxx</td><td>0xe0 - 0xff</td><td>Yes</td></tr>
</table>

---

## API

### pack

Serializes an object into MessagePack. **Returns:** ```okio.BufferedSource```

Instance version:
```kotlin
MoshiPack().pack(anyObject)
```

Static version:
```kotlin
MoshiPack.pack(anyObject)
```

### packToByeArray

If you prefer to get a ```ByteArray``` instead of a ```BufferedSource``` you can use this method.

Instance version only
```kotlin
MoshiPack().packToByteArray(anObject)
```

Static can be done
```kotlin
MoshiPack.pack(anObject).readByteArray()
```

### unpack

Deserializes MessagePack bytes into an Object. **Returns:** ```T: Any```
Works with ```ByteArray``` and ```okio.BufferedSource```

Instance version:
```kotlin
// T must be valid type so Moshi knows what to deserialize to
val unpacked: T = MoshiPack().unpack(byteArray)
```

Static version:
```kotlin
val unpacked: T = MoshiPack.upack(byteArray)
```

Instance version:
```kotlin
val unpacked: T = MoshiPack().unpack(bufferedSource)
```

Static version:
```kotlin
val unpacked: T = MoshiPack.upack(bufferedSource)
```

T can be an Object, a List, a Map, and can include generics. Unlike ```Moshi``` you do not need to specify a parameterized type to deserialize to a List with generics. ```MoshiPack``` can infer the paramterized type for you. 

The following examples are valid for ```MoshiPack```:

A typed List
```kotlin
val listCars: List<Car> = MoshiPack.unpack(carMsgPkBytes)
```

A List of Any
```kotlin
val listCars: List<Any> = MoshiPack.unpack(carMsgPkBytes)
```

An Object
```kotlin
val car: Car = MoshiPack.unpack(carBytes)
```

A Map of Any, Any
```kotlin
val car: Map<Any, Any> = MoshiPack.unpack(carBytes)
```

### msgpackToJson

Convert directly from MessagePack bytes to JSON. Use this method for the most effecient implementation as no objects are instantiated in the process. This uses the ```FormatInterchange``` class to match implementations of ```JsonReader``` and a ```JsonWriter```. If you wanted to say support XML as a direct conversion to and from, you could implement Moshi's ```JsonReader``` and ```JsonWriter``` classes and use the ```FormatInterchange``` class to convert directly to other formats. **Returns** ```String``` containing a JSON representation of the MessagePack data

Instance versions: (takes ```ByteArray``` or ```BufferedSource```)
```kotlin
MoshiPack().msgpackToJson(byteArray)
```

```kotlin
MoshiPack().msgpackToJson(bufferedSource)
```

Static versions: (takes ```ByteArray``` or ```BufferedSource```)
```kotlin
MoshiPack.msgpackToJson(byteArray)
```

```kotlin
MoshiPack.msgpackToJson(bufferedSource)
```

### jsonToMsgpack

Convert directly from JSON to MessagePack bytes. Use this method for the most effecient implementation as no objects are instantiated in the process. **Returns** ```BufferedSource``` 

Instance versions: (takes ```String``` or ```BufferedSource```)
```kotlin
MoshiPack().jsonToMsgpack(jsonString)
```

```kotlin
MoshiPack().jsonToMsgpack(bufferedSource)
```

Static versions: (takes ```String``` or ```BufferedSource```)
```kotlin
MoshiPack.jsonToMsgpack(jsonString)
```

```kotlin
MoshiPack.jsonToMsgpack(bufferedSource)
```

### MoshiPack - constructor + Moshi builder

The ```MoshiPack``` constructor takes an optional ```Moshi.Builder.() -> Unit``` lambda which is applied to the builder that is used to instantiate the ```Moshi``` instance it uses.

Example adding custom adapter:
```kotlin
val moshiPack = MoshiPack({
  add(customAdapter)
})
```

```Moshi``` is also a settable property which can be changed on a ```MoshiPack``` instance:
```kotlin
val m = MoshiPack()
m.moshi = Moshi.Builder().build()
```

The static version of the API also can be passed a lambda to applied to the ```Moshi.Builder``` used to instantiate ```Moshi```:

```kotlin
MoshiPack.pack(someBytes) { add(customAdapter) }
```


### Forcing integers to write as certain format
- new in v1.0.1 

This will force all integers to be packed as the type given.
By default the smallest message pack type is used for integers.
```kotlin
val moshiPack = MoshiPack().apply {
    writerOptions.writeAllIntsAs = MsgpackIntByte.INT_64
}
```


---

Kotiln Support
--------------

Since this library is intended for Kotlin use, the ```moshi-kotlin``` artifact is included as a depedency. A ```KotlinJsonAdapterFactory``` is added by default to the instantiated ```Moshi``` that ```MoshiPack``` uses.
This adapter allows for the use of ```Moshi```'s annotaions in Kotlin. To learn more about it see the [```Moshi```](https://github.com/square/moshi) documentation.

If you'd like to use ```Moshi``` with out a ```KotlinJsonAdapterFactory``` supply a ```Moshi``` instance for ```MoshiPack```:
```kotlin
MoshiPack(moshi = Moshi.Builder().build)
```

ProGuard
--------

From ```Moshi```'s README.md;
If you are using ProGuard you might need to add the following options:
```
-dontwarn okio.**
-dontwarn javax.annotation.**
-keepclasseswithmembers class * {
    @com.squareup.moshi.* <methods>;
}
-keep @com.squareup.moshi.JsonQualifier interface *
-keepclassmembers class kotlin.Metadata {
    public <methods>;
}
```


Retrofit
--------

An example of using the retorfit adapter can be found here:
https://github.com/davethomas11/MoshiPack_AndroidAppExample
