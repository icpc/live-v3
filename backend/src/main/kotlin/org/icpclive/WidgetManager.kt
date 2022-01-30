package org.icpclive

import org.icpclive.api.HideWidgetEvent
import org.icpclive.api.MainScreenEvent
import org.icpclive.api.ShowWidgetEvent
import org.icpclive.api.Widget

object WidgetManager {
    suspend fun showWidget(widget: Widget) {
        processEvent(ShowWidgetEvent(widget))
    }

    suspend fun hideWidget(widgetId: String) {
        processEvent(HideWidgetEvent(widgetId))
    }

    private suspend fun processEvent(e: MainScreenEvent) {
        DataBus.mainScreenEvents.emit(e)
    }
}