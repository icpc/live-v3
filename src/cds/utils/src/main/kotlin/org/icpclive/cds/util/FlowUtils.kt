package org.icpclive.cds.util

import kotlinx.coroutines.*
import kotlinx.coroutines.channels.ProducerScope
import kotlinx.coroutines.flow.*
import java.util.concurrent.atomic.AtomicReference
import kotlin.time.Duration
import kotlin.time.TimeMark
import kotlin.time.TimeSource

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

public fun <T> Flow<T>.onIdle(interval: Duration, block: suspend ProducerScope<T>.() -> Unit) = channelFlow {
    val r = AtomicReference<TimeMark>(TimeSource.Monotonic.markNow())
    val job = launch {
        while (true) {
            delay(interval - r.get().elapsedNow())
            if (r.get().elapsedNow() > interval) {
                block()
                r.set(TimeSource.Monotonic.markNow())
            }
        }
    }
    collect {
        channel.send(it)
        r.set(TimeSource.Monotonic.markNow())
    }
    job.cancel()
}