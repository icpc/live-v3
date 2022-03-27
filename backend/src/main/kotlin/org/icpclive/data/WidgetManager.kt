package org.icpclive.data

import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.icpclive.api.*

object WidgetManager {
    private val mutex = Mutex()
    private var timer = 0L
    private val widgets = mutableListOf<Widget>()

    private suspend fun sendEvent(e: MainScreenEvent) {
        timer++
        widgetsFlowWrite.emit(timer to e)
    }

    suspend fun showWidget(widget: Widget) = mutex.withLock {
        widgets.add(widget)
        sendEvent(ShowWidgetEvent(widget))
    }

    suspend fun hideWidget(widgetId: String) = mutex.withLock {
        if (widgets.removeIf { it.widgetId == widgetId }) {
            sendEvent(HideWidgetEvent(widgetId))
        }
    }

    private suspend fun getWidgetSubscribeEvents() = mutex.withLock {
        timer++
        timer to widgets.toList()
    }

    private val widgetsFlowWrite = MutableSharedFlow<Pair<Long, MainScreenEvent>>(
        replay = 32,
        extraBufferCapacity = 100000,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )

    private val widgetsFlow: Flow<MainScreenEvent> = flow {
        var subscriptionTimer = 0L
        widgetsFlowWrite
            .onSubscription {
                val (timer, widgets) = getWidgetSubscribeEvents()
                subscriptionTimer = timer
                emit(timer to MainScreenSnapshotEvent(widgets))
            }
            .filter { it.first >= subscriptionTimer }
            .map { it.second }
            .collect { emit(it) }
    }

    init {
        DataBus.setMainScreenEvents(widgetsFlow)
    }
}