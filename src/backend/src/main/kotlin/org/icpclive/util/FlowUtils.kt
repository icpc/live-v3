package org.icpclive.util

import kotlinx.coroutines.CompletableDeferred

fun <T> CompletableDeferred<T>.completeOrThrow(value: T) {
    complete(value) || throw IllegalStateException("Double complete of CompletableDeferred")
}