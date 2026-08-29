package org.icpclive.data

import org.icpclive.api.*
import org.icpclive.cds.util.completeOrThrow

class TickerManager : ManagerWithEvents<TickerMessage, TickerEvent>() {
    override fun createAddEvent(item: TickerMessage, showOrder: Long) = AddMessageTickerEvent(item)
    override fun createRemoveEvent(id: String) = RemoveMessageTickerEvent(id)
    override fun createSnapshotEvent(items: List<Ordered<TickerMessage>>) =
        TickerSnapshotEvent(items.map { it.item })

    init {
        DataBus.tickerFlow.completeOrThrow(flow)
    }
}
