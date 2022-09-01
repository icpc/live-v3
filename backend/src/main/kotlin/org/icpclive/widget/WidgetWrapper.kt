package org.icpclive.widget

import kotlinx.coroutines.sync.withLock
import org.icpclive.api.ObjectSettings
import org.icpclive.api.ObjectStatus
import org.icpclive.api.TypeWithId
import org.icpclive.data.Manager

open class WidgetWrapper<SettingsType : ObjectSettings, DataType : TypeWithId>(
    settings: SettingsType,
    private val manager: Manager<DataType>,
    protected val widgetConstructor: (SettingsType) -> DataType,
): AbstractWidgetWrapper<SettingsType, DataType>(settings), SingleWidgetController<SettingsType, DataType> {
    protected var overlayWidgetId: String? = null

    override suspend fun getStatus(): ObjectStatus<SettingsType> = mutex.withLock {
        return ObjectStatus(overlayWidgetId != null, settings)
    }

    suspend fun createWidget() = mutex.withLock {
        widgetConstructor(settings)
    }

    override suspend fun createWidgetAndShow() {
        val widget = widgetConstructor(settings)
        manager.add(widget)
        overlayWidgetId = widget.id
    }

    override suspend fun removeWidget() {
        overlayWidgetId?.let {
            manager.remove(it)
        }
        overlayWidgetId = null
    }
}
