package org.icpclive.util

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.job
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext

fun CoroutineScope.childScope(extraContext: CoroutineContext = EmptyCoroutineContext): CoroutineScope {
    return CoroutineScope(coroutineContext + extraContext + Job(coroutineContext.job))
}
