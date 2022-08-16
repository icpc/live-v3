package org.icpclive.data

import org.icpclive.api.*
import org.icpclive.utils.completeOrThrow

class WidgetManager : ManagerWithEvents<Widget, MainScreenEvent>() {
    override fun createAddEvent(item: Widget) = ShowWidgetEvent(item)
    override fun createRemoveEvent(id: String) = HideWidgetEvent(id)
    override fun createSnapshotEvent(items: List<Widget>) = MainScreenSnapshotEvent(items)

    init {
        DataBus.mainScreenFlow.completeOrThrow(flow)
    }
}