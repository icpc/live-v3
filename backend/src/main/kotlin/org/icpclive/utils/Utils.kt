package org.icpclive.utils

import io.ktor.server.websocket.*
import io.ktor.websocket.*
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.async
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.icpclive.Config
import org.slf4j.LoggerFactory
import java.util.*
import kotlin.reflect.KClass
import kotlin.time.Duration

inline fun <reified T> catchToNull(f: () -> T) = try {
    f()
} catch (_: Exception) {
    null
}

private fun guessDatetimeFormatLocal(time: String) =
    catchToNull { LocalDateTime.parse(time) } ?: catchToNull { LocalDateTime.parse(time.trim().replace(" ", "T")) }


fun guessDatetimeFormat(time: String): Instant =
    catchToNull { Instant.fromEpochMilliseconds(time.toLong() * 1000L) }
        ?: catchToNull { Instant.parse(time) }
        ?: guessDatetimeFormatLocal(time)?.toInstant(TimeZone.currentSystemDefault())
        ?: throw IllegalArgumentException("Failed to parse date: $time")

val Instant.humanReadable: String
    get() = Date(this.toEpochMilliseconds()).toString()

fun tickerFlow(interval: Duration) = flow {
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

fun String.processCreds(): String {
    val prefix = "\$creds."
    return if (startsWith(prefix))
        Config.creds[substring(prefix.length)] ?: throw IllegalStateException("Cred ${substring(prefix.length)} not found")
    else
        this
}


suspend fun DefaultWebSocketServerSession.sendFlow(flow: Flow<String>) {
    val sender = async {
        flow.collect {
            val text = Frame.Text(it)
            outgoing.send(text)
        }
    }
    try {
        for (ignored in incoming) {
            ignored.let {}
        }
    } finally {
        sender.cancel()
    }
}

suspend inline fun <reified T> DefaultWebSocketServerSession.sendJsonFlow(flow: Flow<T>) {
    val formatter = defaultJsonSettings()
    sendFlow(flow.map { formatter.encodeToString(it) })
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