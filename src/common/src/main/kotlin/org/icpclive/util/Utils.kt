package org.icpclive.util

import kotlinx.coroutines.CancellationException
import org.slf4j.LoggerFactory
import java.util.*
import kotlin.reflect.KClass

inline fun <reified T> catchToNull(f: () -> T) = try {
    f()
} catch (e: Exception) {
    null
}

fun getLogger(clazz: KClass<*>) = LoggerFactory.getLogger(clazz.java)!!

fun Properties.getCredentials(key: String, creds: Map<String, String>) = getProperty(key)?.let {
    val prefix = "\$creds."
    if (it.startsWith(prefix)) {
        val name = it.substring(prefix.length)
        creds[name] ?: throw IllegalStateException("Credential $name not found")
    } else {
        it
    }
}?.takeIf { it.isNotEmpty() }

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