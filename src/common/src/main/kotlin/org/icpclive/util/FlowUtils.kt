package org.icpclive.util

import kotlinx.coroutines.*
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.*
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

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
    if (cause is CancellationException) {
        false
    } else {
        log(cause)
        delay(duration)
        true
    }
}

fun <T> CompletableDeferred<T>.completeOrThrow(value: T) {
    complete(value) || throw IllegalStateException("Double complete of CompletableDeferred")
}

inline fun <reified T> loopFlow(interval: Duration, crossinline onError: (Throwable) -> Unit, crossinline block: suspend () -> T) =
    intervalFlow(interval)
        .map { block() }
        .logAndRetryWithDelay(interval) { onError(it) }

fun <T> Flow<Pair<Instant, T>>.toTimedFlow(log: (Instant) -> Unit = {}) : Flow<T> {
    var lastLoggedTime: Instant = Instant.DISTANT_PAST
    return map { (nextEventTime, item) ->
        delay(nextEventTime - Clock.System.now())
        if (nextEventTime - lastLoggedTime > 10.seconds) {
            log(nextEventTime)
            lastLoggedTime = nextEventTime
        }
        item
    }
}
