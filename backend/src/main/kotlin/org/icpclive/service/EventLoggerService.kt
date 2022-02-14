package org.icpclive.service

import kotlinx.coroutines.flow.collect
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.icpclive.DataBus
import org.slf4j.LoggerFactory

class EventLoggerService {
    private val logger = LoggerFactory.getLogger(EventLoggerService::class.java)

    suspend fun run() {
        DataBus.allEvents.collect {
            logger.debug(Json.encodeToString(it))
        }
    }
}