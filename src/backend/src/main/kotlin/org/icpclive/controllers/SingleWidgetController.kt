package org.icpclive.controllers

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import org.icpclive.api.*
import org.icpclive.data.Manager
import org.icpclive.data.Ordered
import org.icpclive.server.ApiActionException
import org.icpclive.util.childScope

abstract class SingleWidgetController<SettingsType : ObjectSettings, DataType : TypeWithId>(
    private val defaultSettings: SettingsType,
    manager: Manager<DataType>,
    parentScope: CoroutineScope,
    private val showOrderCounter: ShowOrderCounter,
    val id: Int? = null,
) : CoroutineScope by parentScope.childScope(Dispatchers.Default), PersistentData<WidgetState<SettingsType>> {
    data class State<SettingsType : ObjectSettings>(
        val settings: SettingsType,
        val showOrder: Long?,
        val alive: Boolean,
    )
    private val state: CompletableDeferred<MutableStateFlow<State<SettingsType>>> = CompletableDeferred()

    override val persistentState: Flow<WidgetState<SettingsType>>
        get() = flow {
            emitAll(state.await()
                .map { WidgetState(it.settings, it.showOrder) }
                .distinctUntilChanged())
        }

    override suspend fun onLoad(data: WidgetState<SettingsType>?) {
        data?.showOrder?.let(showOrderCounter::observe)
        state.complete(
            MutableStateFlow(
                State(data?.settings ?: defaultSettings, showOrder = data?.showOrder, alive = true)
            )
        )
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private val syncJob = launch {
        var prevData: DataType? = null
        state.await()
            .transformWhile { state ->
                // process first non-alive state and stop after it. takeWhile wouldn't emit it, so do it manually
                emit(state)
                state.alive
            }
            .flatMapLatest { (settings, showOrder) ->
                if (showOrder != null) {
                    constructWidgetFlow(settings).map { Ordered(it, showOrder) }
                } else {
                    flowOf(null)
                }
            }
            .collect { orderedWidget ->
                val prev = prevData
                if (prev != null && prev.id != orderedWidget?.item?.id) manager.remove(prev.id)
                if (orderedWidget != null) manager.add(orderedWidget.item, orderedWidget.showOrder)
                prevData = orderedWidget?.item
            }
    }

    suspend fun getStatus(): ObjectStatus<SettingsType> = state.await().value.let {
        ObjectStatus(it.showOrder != null, it.settings, id)
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
        /**
         * Showing an already visible widget keeps its place in the stacking order, so that changing
         * settings of a visible widget doesn't unexpectedly raise it above everything else.
         */
        changeState { if (it.showOrder != null) it else it.copy(showOrder = showOrderCounter.next()) }
    }

    suspend fun hide() {
        changeState { it.copy(showOrder = null) }
    }

    suspend fun destroy() {
        changeState { it.copy(showOrder = null, alive = false) }
        syncJob.join()
        cancel()
    }
}

fun <SettingsType : ObjectSettings, DataType : TypeWithId> CoroutineScope.SingleWidgetController(
    defaultSettings: SettingsType,
    manager: Manager<DataType>,
    showOrderCounter: ShowOrderCounter,
    widgetConstructor: (SettingsType) -> DataType,
    id: Int? = null,
) = object: SingleWidgetController<SettingsType, DataType>(
    defaultSettings, manager, this@SingleWidgetController, showOrderCounter, id
) {
    override suspend fun constructWidgetFlow(settings: SettingsType) = flowOf(widgetConstructor(settings))
}
