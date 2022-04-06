package org.icpclive.data

import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.icpclive.api.*

// TODO: Unify with WidgetManager

object TickerManager : ManagerWithEvents<TickerMessage, TickerEvent>() {
    override fun createAddEvent(item: TickerMessage) = AddMessageTickerEvent(item)
    override fun createRemoveEvent(id: String) = RemoveMessageTickerEvent(id)
    override fun createSnapshotEvent(items: List<TickerMessage>) = TickerSnapshotEvent(items)

    init {
        DataBus.tickerFlow.set(flow)
    }
}
