package org.icpclive.controllers

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import org.icpclive.api.*
import org.icpclive.data.Manager
import org.icpclive.util.childScope

abstract class SingleWidgetController<SettingsType : ObjectSettings, DataType : TypeWithId>(
    settings: SettingsType,
    manager: Manager<DataType>,
    parentScope: CoroutineScope,
    val id: Int? = null,
) : CoroutineScope by parentScope.childScope(Dispatchers.Default) {
    data class State<SettingsType : ObjectSettings, DataType : TypeWithId>(
        val settings: SettingsType,
        val widget: DataType?,
        val showScope: CoroutineScope?,
        // Cleared by destroy(), which lets the manager sync below finish gracefully instead of
        // being cancelled with the removal still pending.
        val alive: Boolean = true,
    )

    private val state = MutableStateFlow(State<SettingsType, DataType>(settings, null, null))

    private val syncJob = launch {
        var prevData: DataType? = null
        state
            .map { it.widget to it.alive }
            .distinctUntilChanged()
            // Emit first, then decide whether to stop, so the final removal is always applied.
            .transformWhile { (widget, alive) -> emit(widget); alive }
            .collect { widget ->
                val prev = prevData
                if (prev != null && prev.id != widget?.id) manager.remove(prev.id)
                if (widget != null) manager.add(widget)
                prevData = widget
            }
    }

    fun getStatus(): ObjectStatus<SettingsType> = state.value.let { (settings, widget, _) ->
        ObjectStatus(widget != null, settings, id)
    }

    fun getSettings() = state.value.settings

    suspend fun previewWidget() = previewWidget(state.value.settings)
    suspend fun previewWidget(previewSettings: SettingsType) = constructWidgetFlow(previewSettings).first()

    abstract suspend fun constructWidgetFlow(settings: SettingsType) : Flow<DataType>

    fun setSettings(newSettings: SettingsType) {
        state.update { it.copy(settings = newSettings) }
    }

    fun show() {
        val showScope = childScope()
        // Only install the scope while alive, so that a show racing destroy() can't write a widget
        // into a state the manager sync has already stopped watching.
        val prev = state.getAndUpdate { if (it.alive) it.copy(showScope = showScope) else it }
        if (!prev.alive) {
            showScope.cancel()
            return
        }
        prev.showScope?.cancel()
        showScope.launch {
            constructWidgetFlow(prev.settings).collect { widget ->
                state.update {
                    if (it.showScope !== showScope) {
                        // someone is already replaced our scope, let's just do nothing, while we are not canceled.
                        it
                    } else {
                        it.copy(widget = widget)
                    }
                }
            }
        }
    }

    fun hide() {
        val prev = state.getAndUpdate {
            it.copy(widget = null, showScope = null)
        }
        prev.showScope?.cancel()
    }

    /**
     * Hides the widget and retires the controller, returning only once the widget is really gone
     * from the manager. The controller can't be used afterwards.
     */
    suspend fun destroy() {
        val prev = state.getAndUpdate {
            it.copy(widget = null, showScope = null, alive = false)
        }
        prev.showScope?.cancel()
        syncJob.join()
        cancel()
    }
}

fun <SettingsType : ObjectSettings, DataType : TypeWithId> CoroutineScope.SingleWidgetController(
    settings: SettingsType,
    manager: Manager<DataType>,
    widgetConstructor: (SettingsType) -> DataType,
    id: Int? = null,
) = object: SingleWidgetController<SettingsType, DataType>(settings, manager, this@SingleWidgetController, id) {
    override suspend fun constructWidgetFlow(settings: SettingsType) = flowOf(widgetConstructor(settings))
}
