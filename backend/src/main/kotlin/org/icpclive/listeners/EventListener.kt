package org.icpclive.listeners

import org.icpclive.api.Event

interface EventListener {
    suspend fun processEvent(e: Event)
}