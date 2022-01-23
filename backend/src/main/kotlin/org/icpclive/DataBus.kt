package org.icpclive

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.merge
import org.icpclive.api.MainScreenEvent
import org.icpclive.api.QueueEvent

object DataBus {
    val mainScreenEvents = MutableSharedFlow<MainScreenEvent>(
        extraBufferCapacity = 1024,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    val queueEvents = MutableSharedFlow<QueueEvent>(
        extraBufferCapacity = 1024,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    @OptIn(ExperimentalCoroutinesApi::class)
    val allEvents get() = merge(mainScreenEvents, queueEvents)
}