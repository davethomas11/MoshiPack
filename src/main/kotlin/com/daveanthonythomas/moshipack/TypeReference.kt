package com.daveanthonythomas.moshipack

import com.squareup.moshi.Types
import sun.reflect.generics.reflectiveObjects.ParameterizedTypeImpl
import sun.reflect.generics.reflectiveObjects.WildcardTypeImpl
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

        val args = (superclass as ParameterizedType).actualTypeArguments
        val theType = args[0]

        if (theType is ParameterizedType) {
            val bounded = theType.actualTypeArguments.map {
                if (it is WildcardTypeImpl) {
                    if (it.upperBounds?.size?.compareTo(0) ?: 0 > 0) {
                        it.upperBounds[0]
                    } else it
                } else it
            }
            this.type = Types.newParameterizedType(theType.rawType, *bounded.toTypedArray())
        } else {
            this.type = theType
        }
    }
}