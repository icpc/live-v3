package org.icpclive.service

import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import org.icpclive.api.AnalyticsEvent
import org.icpclive.data.DataBus
import org.icpclive.utils.completeOrThrow
import org.icpclive.utils.getLogger

class AnalyticsEventsService {
    private val updatesFlow = MutableSharedFlow<AnalyticsEvent>(
        extraBufferCapacity = 50000,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
        replay = 50
    )

    suspend fun run(rawEvents: Flow<AnalyticsEvent>) {
        rawEvents.collect {
            updatesFlow.emit(it)
        }
    }


    init {
        DataBus.analyticsEventFlow.completeOrThrow(updatesFlow)
    }

    companion object {
        val logger = getLogger(AnalyticsEvent::class)
    }
}
