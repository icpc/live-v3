package org.icpclive.util

import kotlinx.coroutines.*
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.*
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

public fun <T> Flow<T>.logAndRetryWithDelay(duration: Duration, log: (Throwable) -> Unit): Flow<T> = retryWhen { cause: Throwable, _: Long ->
    if (cause is CancellationException) {
        false
    } else {
        log(cause)
        delay(duration)
        true
    }
}

public fun <T> CompletableDeferred<T>.completeOrThrow(value: T) {
    complete(value) || throw IllegalStateException("Double complete of CompletableDeferred")
}

public inline fun <reified T> loopFlow(interval: Duration, crossinline onError: (Throwable) -> Unit, crossinline block: suspend () -> T): Flow<T> =
    flow {
        while (true) {
            emit(block())
            delay(interval)
        }
    }.logAndRetryWithDelay(interval) { onError(it) }

