package org.icpclive.controllers

import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.icpclive.api.*
import org.icpclive.cds.util.getLogger
import org.icpclive.data.Manager

private val logger by getLogger()

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

    suspend fun previewWidget(previewSettings: SettingsType = settings) = mutex.withLock {
        constructWidget(previewSettings)
    }

    abstract suspend fun constructWidget(settings: SettingsType) : DataType
    abstract suspend fun onDelete(id:Int)

    open suspend fun createWidgetAndShow(settings: SettingsType) {
        val widget = constructWidget(settings)
        if (overlayWidgetId != null && overlayWidgetId != widget.id) {
            logger.warning { "Controller $id is currently showing ${overlayWidgetId}, but was asked to show ${widget.id}, would hide first one silently." }
            manager.remove(overlayWidgetId!!)
        }
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
        cancel()
        showImpl()
    }

    suspend fun show(newSettings: SettingsType) = mutex.withLock {
        cancel()
        settings = newSettings
        showImpl()
    }

    private suspend fun showImpl() {
        widgetShowScope = CoroutineScope(Dispatchers.Default)
        createWidgetAndShow(settings)
    }

    private suspend fun cancel() {
        widgetShowScope?.cancel()
        widgetShowScope = null
    }

    suspend fun hide() = mutex.withLock {
        removeWidget()
        cancel()
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
