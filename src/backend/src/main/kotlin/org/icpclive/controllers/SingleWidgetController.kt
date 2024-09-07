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

abstract class SingleWidgetController<SettingsType : ObjectSettings, DataType : TypeWithId>(
    private var settings: SettingsType,
    private val manager: Manager<in DataType>,
    val id: Int? = null,
) {
    private val mutex = Mutex()
    private val widgetScope = CoroutineScope(Dispatchers.Default)
    private var widgetShowScope: CoroutineScope? = null
    private var overlayWidgetId: String? = null

    suspend fun getStatus(): ObjectStatus<SettingsType> = mutex.withLock {
        return ObjectStatus(overlayWidgetId != null, settings, id)
    }

    suspend fun previewWidget() = mutex.withLock {
        constructWidget(settings)
    }

    abstract suspend fun constructWidget(settings: SettingsType) : DataType
    abstract suspend fun onDelete(id:Int)

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

    suspend fun setSettings(newSettings: SettingsType) = mutex.withLock {
        checkSettings(newSettings)
        settings = newSettings
    }

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
            onDelete(id)
        }
        widgetScope.cancel()
    }
    // throws if settings are bad
    open suspend fun checkSettings(settings: SettingsType) {}
}
fun <SettingsType : ObjectSettings, DataType : TypeWithId> SingleWidgetController(
    settings: SettingsType,
    manager: Manager<DataType>,
    widgetConstructor: (SettingsType) -> DataType,
    id: Int? = null,
    onDeleteCallback: suspend (Int) -> Unit = {}
) = object: SingleWidgetController<SettingsType, DataType>(settings, manager, id) {
    override suspend fun constructWidget(settings: SettingsType) = widgetConstructor(settings)

    override suspend fun onDelete(id: Int) = onDeleteCallback(id)
}
