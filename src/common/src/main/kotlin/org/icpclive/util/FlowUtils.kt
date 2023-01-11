package org.icpclive.util

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlin.time.Duration

fun intervalFlow(interval: Duration) = flow {
    while (true) {
        emit(null)
        delay(interval)
    }
}

/**
 * Delivers all events to all current and future subscribers
 */
fun <T> reliableSharedFlow() = MutableSharedFlow<T>(
    replay = Int.MAX_VALUE,
    onBufferOverflow = BufferOverflow.SUSPEND,
)

fun <T> Flow<T>.logAndRetryWithDelay(duration: Duration, log: (Throwable) -> Unit) = retryWhen { cause: Throwable, _: Long ->
    log(cause)
    delay(duration)
    true
}

fun <T> CompletableDeferred<T>.completeOrThrow(value: T) {
    complete(value) || throw IllegalStateException("Double complete of CompletableDeferred")
}

suspend fun <T> MutableSharedFlow<T>.awaitSubscribers(count: Int = 1) {
    subscriptionCount.dropWhile { it < count }.first()
}

inline fun <reified T> loopFlow(interval: Duration, crossinline onError: (Throwable) -> Unit, crossinline block: suspend () -> T) =
    intervalFlow(interval)
        .map { block() }
        .logAndRetryWithDelay(interval) { onError(it) }