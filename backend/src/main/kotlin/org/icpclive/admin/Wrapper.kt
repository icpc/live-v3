package org.icpclive.admin

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.icpclive.api.ObjectSettings
import org.icpclive.api.ObjectStatus
import org.icpclive.api.TypeWithId
import org.icpclive.data.Manager

class Wrapper<SettingsType : ObjectSettings, DataType : TypeWithId>(
    private val createWidget: (SettingsType) -> DataType,
    private var settings: SettingsType,
    private val manager: Manager<DataType>,
    val id: Int? = null
) {
    private val mutex = Mutex()

    private var widgetId: String? = null

    suspend fun getStatus(): ObjectStatus<SettingsType> = mutex.withLock {
        return ObjectStatus(widgetId != null, settings, id)
    }

    suspend fun getSettings() = mutex.withLock {
        settings
    }

    suspend fun set(newSettings: SettingsType) {
        mutex.withLock {
            settings = newSettings
        }
    }

    suspend fun show() {
        mutex.withLock {
            if (widgetId != null) {
                widgetId?.let {
                    manager.remove(it)
                }
                widgetId = null
            }
            val widget = createWidget(settings)
            manager.add(widget)
            widgetId = widget.id
        }
    }

    suspend fun show(newSettings: SettingsType) {
        mutex.withLock {
            if (widgetId != null) {
                widgetId?.let {
                    manager.remove(it)
                }
                widgetId = null
            }
            settings = newSettings
            val widget = createWidget(settings)
            manager.add(widget)
            widgetId = widget.id
        }
    }

    suspend fun hide() {
        mutex.withLock {
            widgetId?.let {
                manager.remove(it)
            }
            widgetId = null
        }
    }
}