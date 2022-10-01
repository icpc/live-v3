package org.icpclive.controllers

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.icpclive.api.ObjectSettings
import org.icpclive.api.ObjectStatus
import org.icpclive.api.TypeWithId
import org.icpclive.data.Manager

open class SingleWidgetController<SettingsType : ObjectSettings, DataType : TypeWithId>(
    private var settings: SettingsType,
    private val manager: Manager<DataType>,
    private val widgetConstructor: (SettingsType) -> DataType,
    val id: Int? = null,
    private val onDeleteCallback: suspend (Int) -> Unit = {}
) {
    private val mutex = Mutex()
    private val widgetScope = CoroutineScope(Dispatchers.Default)
    private var widgetShowScope: CoroutineScope? = null
    private var overlayWidgetId: String? = null

    suspend fun getStatus(): ObjectStatus<SettingsType> = mutex.withLock {
        return ObjectStatus(overlayWidgetId != null, settings, id)
    }

    suspend fun previewWidget() = mutex.withLock {
        widgetConstructor(settings)
    }

    protected open suspend fun constructWidget(settings: SettingsType) = widgetConstructor(settings)

    open suspend fun createWidgetAndShow(settings: SettingsType) {
        val widget = constructWidget(settings)
        manager.add(widget)
        overlayWidgetId = widget.id
    }

    open suspend fun removeWidget() {
        overlayWidgetId?.let { manager.remove(it) }
        overlayWidgetId = null
    }

    fun launchWhileWidgetExists(block: suspend () -> Unit) = widgetScope.launch { block() }
    fun launchWhileWidgetShown(block: suspend () -> Unit) = widgetShowScope?.launch { block() }

    suspend fun getSettings() = mutex.withLock { settings }

    suspend fun setSettings(newSettings: SettingsType) = mutex.withLock { settings = newSettings }

    suspend fun show() = mutex.withLock {
        hideImpl()
        showImpl()
    }

    suspend fun show(newSettings: SettingsType) = mutex.withLock {
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

    suspend fun hide() = mutex.withLock {
        hideImpl()
    }

    open suspend fun onDelete() {
        if (id != null) {
            onDeleteCallback(id)
        }
        widgetScope.cancel()
    }
}
