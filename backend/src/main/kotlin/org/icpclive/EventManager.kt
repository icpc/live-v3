package org.icpclive

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.icpclive.api.Event
import org.icpclive.listeners.EventListener

object EventManager {
    private val listeners = mutableListOf<EventListener>()
    private val mutex = Mutex()

    suspend fun registerListener(listener: EventListener) = mutex.withLock {
        listeners.add(listener)
    }
    suspend fun unregisterListener(listener: EventListener) = mutex.withLock {
        listeners.remove(listener)
    }
    suspend fun processEvent(event: Event) = mutex.withLock {
        for (listener in listeners) {
            listener.processEvent(event)
        }
    }
}