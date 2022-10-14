package org.icpclive.common.util

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.datetime.*
import kotlinx.datetime.TimeZone
import kotlinx.serialization.json.Json
import org.slf4j.LoggerFactory
import java.util.*
import kotlin.reflect.KClass
import kotlin.time.Duration

inline fun <reified T> catchToNull(f: () -> T) = try {
    f()
} catch (e: Exception) {
    suppressIfNotCancellation(e)
}

private fun guessDatetimeFormatLocal(time: String) =
    catchToNull { LocalDateTime.parse(time) } ?: catchToNull { LocalDateTime.parse(time.trim().replace(" ", "T")) }


fun guessDatetimeFormat(time: String): Instant =
    Clock.System.now().takeIf { time == "now" }
        ?: catchToNull { Instant.fromEpochMilliseconds(time.toLong() * 1000L) }
        ?: catchToNull { Instant.parse(time) }
        ?: guessDatetimeFormatLocal(time)?.toInstant(TimeZone.currentSystemDefault())
        ?: throw IllegalArgumentException("Failed to parse date: $time")

val Instant.humanReadable: String
    get() = Date(this.toEpochMilliseconds()).toString()

fun intervalFlow(interval: Duration) = flow {
    while (true) {
        emit(null)
        delay(interval)
    }
}

fun defaultJsonSettings() = Json {
    encodeDefaults = true
    isLenient = true
    allowSpecialFloatingPointValues = true
    allowStructuredMapKeys = true
    prettyPrint = false
    useArrayPolymorphism = false
    explicitNulls = false
}

fun getLogger(clazz: KClass<*>) = LoggerFactory.getLogger(clazz.java)!!

fun <T> CompletableDeferred<T>.completeOrThrow(value: T) {
    complete(value) || throw IllegalStateException("Double complete of CompletableDeferred")
}

/**
 * Delivers all events to all current and future subscribers
 */
fun <T> reliableSharedFlow() = MutableSharedFlow<T>(
    replay = Int.MAX_VALUE,
    onBufferOverflow = BufferOverflow.SUSPEND,
)

suspend fun <T> MutableSharedFlow<T>.awaitSubscribers(count: Int = 1) {
    subscriptionCount.dropWhile { it < count }.first()
}

fun suppressIfNotCancellation(e: Exception) = if (e is CancellationException) throw e else null

fun <T> Flow<T>.logAndRetryWithDelay(duration: Duration, log: (Throwable) -> Unit) = retryWhen { cause: Throwable, _: Long ->
    log(cause)
    delay(duration)
    true
}

fun Properties.getCredentials(key: String, creds: Map<String, String>) = getProperty(key)?.let {
    val prefix = "\$creds."
    if (it.startsWith(prefix)) {
        val name = it.substring(prefix.length)
        creds[name] ?: throw IllegalStateException("Credential $name not found")
    } else {
        it
    }
}?.takeIf { it.isNotEmpty() }
