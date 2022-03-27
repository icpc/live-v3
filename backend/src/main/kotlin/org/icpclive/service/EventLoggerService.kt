package org.icpclive.service

import kotlinx.coroutines.flow.collect
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.icpclive.data.DataBus
import org.icpclive.utils.getLogger

class EventLoggerService {
    companion object {
        private val logger = getLogger(EventLoggerService::class)
    }

    suspend fun run() {
        DataBus.allEvents.collect {
            logger.debug(Json.encodeToString(it))
        }
    }
}