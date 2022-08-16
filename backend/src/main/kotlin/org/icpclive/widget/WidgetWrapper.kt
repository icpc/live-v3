package org.icpclive.widget

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.icpclive.api.ObjectSettings
import org.icpclive.api.ObjectStatus
import org.icpclive.api.TypeWithId
import org.icpclive.data.Manager

open class WidgetWrapper<SettingsType : ObjectSettings, DataType : TypeWithId>(
    protected var settings: SettingsType,
    private val manager: Manager<DataType>,
    protected val widgetConstructor: (SettingsType) -> DataType,
) {
    protected val mutex = Mutex()
    protected var overlayWidgetId: String? = null

    open suspend fun getStatus(): ObjectStatus<SettingsType> = mutex.withLock {
        return ObjectStatus(overlayWidgetId != null, settings)
    }

    suspend fun createWidget() = mutex.withLock {
        widgetConstructor(settings)
    }

    suspend fun getSettings() = mutex.withLock {
        settings
    }

    suspend fun setSettings(newSettings: SettingsType) {
        mutex.withLock {
            settings = newSettings
        }
    }

    suspend fun show() {
        mutex.withLock {
            removeWidget()
            createWidgetAndShow()
        }
    }

    suspend fun show(newSettings: SettingsType) = mutex.withLock {
        removeWidget()
        settings = newSettings
        createWidgetAndShow()
    }

    suspend fun hide() = mutex.withLock {
        removeWidget()
    }

    private suspend fun createWidgetAndShow() {
        val widget = widgetConstructor(settings)
        manager.add(widget)
        overlayWidgetId = widget.id
    }

    private suspend fun removeWidget() {
        overlayWidgetId?.let {
            manager.remove(it)
        }
        overlayWidgetId = null
    }
}
