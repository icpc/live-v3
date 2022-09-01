package org.icpclive.widget

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.icpclive.api.ObjectSettings
import org.icpclive.api.TypeWithId

abstract class AbstractWidgetWrapper<SettingsType : ObjectSettings, DataType : TypeWithId>(
    protected var settings: SettingsType,
) {
    protected val mutex = Mutex()
    val scope = CoroutineScope(Dispatchers.Default)

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

    protected abstract suspend fun createWidgetAndShow()
    protected abstract suspend fun removeWidget()
}
