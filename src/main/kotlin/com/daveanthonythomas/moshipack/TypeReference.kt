package com.daveanthonythomas.moshipack

import java.lang.reflect.ParameterizedType
import java.lang.reflect.Type

/**
 * Base on http://gafter.blogspot.ca/2006/12/super-type-tokens.html
 * To help with type erasure of generics in reified types.
 * This class helps us so we can unpack to for example a List<Int>
 * with out having to supply a ParameterizedType to MoshiPack.
 * MoshiPack uses this to figure out the ParameterizedType iteself
 * for Moshi.
 */
abstract class TypeReference<T> {
    val type: Type

    init {
        val superclass = javaClass.genericSuperclass
        if (superclass is Class<*>) {
            throw RuntimeException("Missing type parameter.")
        }

        // TODO: Support more than one parameter in base Generic type
        val args = (superclass as ParameterizedType).actualTypeArguments
        this.type = args[0]
    }
}