package org.icpclive.admin.overlayEvents

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.icpclive.api.Event
import org.icpclive.listeners.EventListener

class OverlayEventsListener : EventListener {
    private val events = mutableListOf<String>()
    private val mutex = Mutex()
    override suspend fun processEvent(e: Event) {
        mutex.withLock {
            events.add(Json.encodeToString(e))
        }
    }
    suspend fun getEventsToShow(count: Int = 100) = mutex.withLock {
        events.subList(maxOf(events.size - count, 0), events.size).toList()
    }
}