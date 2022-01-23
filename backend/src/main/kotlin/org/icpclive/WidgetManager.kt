package org.icpclive

import org.icpclive.api.*

object WidgetManager {
    suspend fun showWidget(widget: Widget) {
        processEvent(ShowWidgetEvent(widget))
    }
    suspend fun hideWidget(widgetId: String) {
        processEvent(HideWidgetEvent(widgetId))
    }

    private suspend fun processEvent(e: Event) {
        EventManager.processEvent(e)
    }
}