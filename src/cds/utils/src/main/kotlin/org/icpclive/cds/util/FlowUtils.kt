package org.icpclive.cds.util

import kotlinx.coroutines.*
import kotlinx.coroutines.channels.ProducerScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.selects.onTimeout
import kotlinx.coroutines.selects.select
import kotlin.concurrent.atomics.AtomicInt
import kotlin.concurrent.atomics.ExperimentalAtomicApi
import kotlin.concurrent.atomics.decrementAndFetch
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

@OptIn(ExperimentalCoroutinesApi::class)
public fun <T> Flow<T>.onIdle(interval: Duration, block: suspend ProducerScope<T>.() -> Unit): Flow<T> = channelFlow {
    val data = produceIn(this)
    var finished = false
    while (!finished) {
        select {
            data.onReceiveCatching {
                if (it.isSuccess) {
                    send(it.getOrThrow())
                } else {
                    finished = true
                }
            }
            onTimeout(interval) {
                block()
            }
        }
    }
}

public fun <T> CompletableDeferred<T>.completeOrThrow(value: T) {
    complete(value) || throw IllegalStateException("Double complete of CompletableDeferred")
}

public class SharedFlowSubscriptionScope<T> @PublishedApi internal constructor(
    @PublishedApi internal val flow: SharedFlow<T>,
    private val subscriptionWaitingFlow: MutableStateFlow<Int>
) {

    @PublishedApi
    internal fun acquire(count: Int) {
        subscriptionWaitingFlow.update { it + count }
    }
    @PublishedApi
    internal fun release() {
        subscriptionWaitingFlow.update { it - 1 }
    }

    @OptIn(ExperimentalAtomicApi::class)
    public inline fun withSubscription(count: Int = 1, block: (Flow<T>) -> Unit) {
        acquire(count)
        val remaining = AtomicInt(count)
        block(flow.onSubscription {
            require(remaining.decrementAndFetch() >= 0) { "Can't subscribe more than $count times"}
            release()
        })
    }
}

public inline fun <T> Flow<T>.shareWith(scope: CoroutineScope, subscribers: SharedFlowSubscriptionScope<T>.() -> Unit) {
    val subscriptionWaitingFlow = MutableStateFlow(1)
    val starter = SharingStarted {
        subscriptionWaitingFlow.transformWhile {
            if (it == 0) {
                emit(SharingCommand.START)
                false
            } else {
                true
            }
        }
    }
    SharedFlowSubscriptionScope(shareIn(scope, starter), subscriptionWaitingFlow).apply {
        subscribers()
        subscriptionWaitingFlow.update { it - 1 }
    }
}