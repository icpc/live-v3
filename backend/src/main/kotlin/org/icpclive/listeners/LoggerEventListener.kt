package org.icpclive.listeners

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.icpclive.api.Event
import org.slf4j.LoggerFactory

class LoggerEventListener : EventListener {
    private val logger = LoggerFactory.getLogger(LoggerEventListener::class.java)
    override suspend fun processEvent(e: Event) {
        logger.info(Json.encodeToString(e))
    }
}