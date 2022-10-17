package org.icpclive.common.util

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

fun suppressIfNotCancellation(e: Exception) = if (e is CancellationException) throw e else null


fun Properties.getCredentials(key: String, creds: Map<String, String>) = getProperty(key)?.let {
    val prefix = "\$creds."
    if (it.startsWith(prefix)) {
        val name = it.substring(prefix.length)
        creds[name] ?: throw IllegalStateException("Credential $name not found")
    } else {
        it
    }
}?.takeIf { it.isNotEmpty() }
