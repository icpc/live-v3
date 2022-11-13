package org.icpclive.data

import org.icpclive.api.*
import org.icpclive.util.completeOrThrow

class TickerManager : ManagerWithEvents<TickerMessage, TickerEvent>() {
    override fun createAddEvent(item: TickerMessage) = AddMessageTickerEvent(item)
    override fun createRemoveEvent(id: String) = RemoveMessageTickerEvent(id)
    override fun createSnapshotEvent(items: List<TickerMessage>) = TickerSnapshotEvent(items)

    init {
        DataBus.tickerFlow.completeOrThrow(flow)
    }
}
