package org.icpclive.data

import org.icpclive.api.*

object TickerManager : ManagerWithEvents<TickerMessage, TickerEvent>() {
    override fun createAddEvent(item: TickerMessage) = AddMessageTickerEvent(item)
    override fun createRemoveEvent(id: String) = RemoveMessageTickerEvent(id)
    override fun createSnapshotEvent(items: List<TickerMessage>) = TickerSnapshotEvent(items)

    init {
        DataBus.tickerFlow.complete(flow)
    }
}
