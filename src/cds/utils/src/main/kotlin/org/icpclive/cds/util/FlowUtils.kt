package org.icpclive.cds.util

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlin.time.Duration

public fun <T> Flow<T>.logAndRetryWithDelay(duration: Duration, log: (Throwable) -> Unit): Flow<T> = retryWhen { cause: Throwable, _: Long ->
    if (cause is CancellationException) {
        false
    } else {
        log(cause)
        delay(duration)
        true
    }
}

public inline fun <T> loopFlow(interval: Duration, crossinline onError: (Throwable) -> Unit, crossinline block: suspend () -> T): Flow<T> =
    flow {
        while (true) {
            emit(block())
            delay(interval)
        }
    }.logAndRetryWithDelay(interval) { onError(it) }

public fun <T> runCatchingIfNotCancellation(block: () -> T): Result<T> = runCatching { block() }.also {
    val exception = it.exceptionOrNull()
    if (exception is CancellationException) throw exception
}