package org.icpclive.background

import kotlinx.coroutines.*

object BackgroundProcessManager {
    // TODO: use some other scope?
    @OptIn(DelicateCoroutinesApi::class)
    fun launch(block: suspend CoroutineScope.() -> Unit) = GlobalScope.launch(block = block)
}