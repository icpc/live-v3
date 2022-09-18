package org.icpclive.widget

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.icpclive.api.ObjectSettings
import org.icpclive.api.TypeWithId

abstract class AbstractWidgetWrapper<SettingsType : ObjectSettings, DataType : TypeWithId>(
    protected var settings: SettingsType,
) : SingleWidgetController<SettingsType, DataType> {
    protected val mutex = Mutex()
    private val widgetScope = CoroutineScope(Dispatchers.Default)
    private var widgetShowScope: CoroutineScope? = null

    fun launchWhileWidgetExists(block: suspend () -> Unit) = widgetScope.launch { block() }
    fun launchWhileWidgetShown(block: suspend () -> Unit) = widgetShowScope?.launch { block() }

    suspend fun getSettings() = mutex.withLock {
        settings
    }

    override suspend fun setSettings(newSettings: SettingsType) = mutex.withLock {
        settings = newSettings
    }

    override suspend fun show() = mutex.withLock {
        hideImpl()
        showImpl()
    }


    override suspend fun show(newSettings: SettingsType) = mutex.withLock {
        hideImpl()
        settings = newSettings
        showImpl()
    }

    private suspend fun showImpl() {
        widgetShowScope = CoroutineScope(Dispatchers.Default)
        createWidgetAndShow(settings)
    }
    private suspend fun hideImpl() {
        removeWidget()
        widgetShowScope?.cancel()
        widgetShowScope = null
    }

    override suspend fun hide() = mutex.withLock {
        hideImpl()
    }

    open suspend fun onDelete() {
        widgetScope.cancel()
    }

    protected abstract suspend fun createWidgetAndShow(settings: SettingsType)
    protected abstract suspend fun removeWidget()
}
