package org.icpclive.data

import org.icpclive.api.*
import org.icpclive.cds.util.completeOrThrow

class TickerManager : ManagerWithEvents<TickerMessage, TickerEvent>() {
    override fun createAddEvent(item: TickerMessage, showOrder: Long): AddMessageTickerEvent {
        return AddMessageTickerEvent(item, showOrder)
    }
    override fun createRemoveEvent(id: String): RemoveMessageTickerEvent {
        return RemoveMessageTickerEvent(id)
    }
    override fun createSnapshotEvent(items: List<Ordered<TickerMessage>>): TickerSnapshotEvent {
        return TickerSnapshotEvent(items.map { OrderedTickerMessage(it.item, it.showOrder) })
    }

    init {
        DataBus.tickerFlow.completeOrThrow(flow)
    }
}
