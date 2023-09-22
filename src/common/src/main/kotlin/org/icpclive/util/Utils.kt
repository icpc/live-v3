package org.icpclive.util

import org.slf4j.LoggerFactory
import kotlin.reflect.KClass

inline fun <reified T> catchToNull(f: () -> T) = try {
    f()
} catch (e: Exception) {
    null
}

fun getLogger(clazz: KClass<*>) = LoggerFactory.getLogger(clazz.java)!!

class Enumerator<K> {
    val keys = mutableMapOf<K, Int>()

    operator fun get(key: K) = keys.getOrPut(key) { keys.size + 1 }
}

fun <T> Iterable<T>.atMostOne(predicate: (T) -> Boolean) : T? {
    var single: T? = null
    var found = false
    for (element in this) {
        if (predicate(element)) {
            if (found) throw IllegalArgumentException("Collection contains more than one matching element.")
            single = element
            found = true
        }
    }
    return single
}