MoshiPack 
=========

[![CircleCI](https://circleci.com/gh/davethomas11/MoshiPack/tree/master.svg?style=svg)](https://circleci.com/gh/davethomas11/MoshiPack/tree/master) [![Release](https://jitpack.io/v/davethomas11/MoshiPack.svg)](https://jitpack.io/#davethomas11/MoshiPack)

This is a Kotilin implementation of MessagePack serialization and deserialization built ontop of Moshi to take advantage of Moshi's type adapters and utilizes okio for reading and writing MessagePack bytes.

The library is intended to be consumed in a Kotlin project, and is not intended for Java use.

Inspired by https://twitter.com/kaushikgopal/status/961426258818039808

See [Moshi](https://github.com/square/moshi) for adapter usage and reference.

----

## Status -> In development ( Getting closer ) ```Broiling```
Currently testing against multiple data types. 

---

### Convert an object to MessagePack format

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
MoshiPack().pack(anObject).readByteArray()
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

