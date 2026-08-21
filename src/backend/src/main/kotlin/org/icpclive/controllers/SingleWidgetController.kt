package org.icpclive.controllers

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import org.icpclive.api.*
import org.icpclive.data.Manager
import org.icpclive.server.ApiActionException
import org.icpclive.util.childScope

abstract class SingleWidgetController<SettingsType : ObjectSettings, DataType : TypeWithId>(
    private val defaultSettings: SettingsType,
    manager: Manager<DataType>,
    parentScope: CoroutineScope,
    val id: Int? = null,
) : CoroutineScope by parentScope.childScope(Dispatchers.Default), PersistentData<WidgetState<SettingsType>> {
    data class State<SettingsType : ObjectSettings>(
        val settings: SettingsType,
        val visible: Boolean,
        val alive: Boolean,
    )

    private val state: CompletableDeferred<MutableStateFlow<State<SettingsType>>> = CompletableDeferred()

    override val persistentState: Flow<WidgetState<SettingsType>>
        get() = flow {
            emitAll(state.await()
                .map { WidgetState(it.settings, it.visible) }
                .distinctUntilChanged())
        }

    override suspend fun onLoad(data: WidgetState<SettingsType>?) {
        val newState: State<SettingsType> = State(data?.settings ?: defaultSettings, visible = false, alive = true)
        state.complete(MutableStateFlow(newState))
        if (data?.visible == true) {
            show()
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private val syncJob = launch {
        var prevData: DataType? = null
        state.await()
            .transformWhile { state ->
                // process first non-alive state and stop after it. takeWhile wouldn't emit it, so do it manually
                emit(state.settings.takeIf { state.visible })
                state.alive
            }
            .flatMapLatest { settings ->
                if (settings != null) {
                    constructWidgetFlow(settings)
                } else {
                    flowOf(null)
                }
            }
            .collect { widget ->
                val prev = prevData
                if (prev != null && prev.id != widget?.id) manager.remove(prev.id)
                if (widget != null) manager.add(widget)
                prevData = widget
            }
    }

    suspend fun getStatus(): ObjectStatus<SettingsType> = state.await().value.let { (settings, visible) ->
        ObjectStatus(visible, settings, id)
    }

    suspend fun previewWidget() = previewWidget(state.await().value.settings)
    suspend fun previewWidget(previewSettings: SettingsType) = constructWidgetFlow(previewSettings).first()

    abstract suspend fun constructWidgetFlow(settings: SettingsType) : Flow<DataType>

    private suspend inline fun changeState(block: (State<SettingsType>) -> State<SettingsType>) {
        state.await().update {
            if (!it.alive) {
                throw ApiActionException("Can't change widget state, it was just deleted")
            }
            block(it)
        }
    }

    suspend fun setSettings(newSettings: SettingsType) {
        changeState { it.copy(settings = newSettings) }
    }

    suspend fun show() {
        changeState { it.copy(visible = true) }
    }

    suspend fun hide() {
        changeState { it.copy(visible = false) }
    }

    suspend fun destroy() {
        changeState { it.copy(visible = false, alive = false) }
        syncJob.join()
        cancel()
    }
}

fun <SettingsType : ObjectSettings, DataType : TypeWithId> CoroutineScope.SingleWidgetController(
    defaultSettings: SettingsType,
    manager: Manager<DataType>,
    widgetConstructor: (SettingsType) -> DataType,
    id: Int? = null,
) = object: SingleWidgetController<SettingsType, DataType>(defaultSettings, manager, this@SingleWidgetController, id) {
    override suspend fun constructWidgetFlow(settings: SettingsType) = flowOf(widgetConstructor(settings))
}
