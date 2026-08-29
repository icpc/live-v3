package org.icpclive.controllers

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import org.icpclive.api.ObjectSettings
import org.icpclive.api.TypeWithId
import org.icpclive.data.Manager
import org.icpclive.server.ApiActionException
import org.icpclive.util.childScope
import kotlin.concurrent.atomics.AtomicInt
import kotlin.concurrent.atomics.incrementAndFetch
import kotlin.time.Duration

class PresetsController<SettingsType : ObjectSettings, OverlayWidgetType : TypeWithId>(
    private val widgetManager: Manager<OverlayWidgetType>,
    parentScope: CoroutineScope,
    private val showOrderCounter: ShowOrderCounter,
    private val widgetConstructor: (SettingsType) -> OverlayWidgetType,
) : CoroutineScope by parentScope.childScope(Dispatchers.Default), PersistentData<List<WidgetState<SettingsType>>> {

    private val currentID = AtomicInt(0)

    private class Entry<S : ObjectSettings, W : TypeWithId>(
        val controller: SingleWidgetController<S, W>,
        val onDelete: (suspend (Int) -> Unit)? = null,
    )

    private val entries: CompletableDeferred<MutableStateFlow<List<Entry<SettingsType, OverlayWidgetType>>>> =
        CompletableDeferred()

    @OptIn(ExperimentalCoroutinesApi::class)
    override val persistentState: Flow<List<WidgetState<SettingsType>>>
        get() = flow {
            emitAll(entries.await().flatMapLatest { list ->
                val flows = list.filter { it.onDelete == null }.map { it.controller.persistentState }
                if (flows.isEmpty()) flowOf(emptyList()) else combine(flows) { it.toList() }
            })
        }.distinctUntilChanged()

    override suspend fun onLoad(data: List<WidgetState<SettingsType>>?) {
        val loaded = data?.map { state ->
            Entry(
                SingleWidgetController(
                    state.settings,
                    widgetManager,
                    showOrderCounter,
                    widgetConstructor,
                    currentID.incrementAndFetch()
                ).also { it.onLoad(state) }
            )
        }.orEmpty()
        entries.complete(MutableStateFlow(loaded))
    }

    suspend fun getStatus() = entries.await().value.map { it.controller.getStatus() }

    suspend fun previewWidget(id: Int) = findById(id).previewWidget()

    private suspend fun createWidgetImpl(settings: SettingsType, onDelete: (suspend (Int) -> Unit)? = null): Int {
        val id = currentID.incrementAndFetch()
        val controller = SingleWidgetController(settings, widgetManager, showOrderCounter, widgetConstructor, id)
        controller.onLoad(null)
        entries.await().update { it.plus(Entry(controller, onDelete)) }
        return id
    }

    suspend fun createWidget(settings: SettingsType): Int = createWidgetImpl(settings)

    suspend fun createWidget(settings: SettingsType, ttl: Duration, onDelete: suspend (Int) -> Unit): Int {
        val id = createWidgetImpl(settings, onDelete)
        launch {
            delay(ttl)
            delete(id)
        }
        return id
    }

    suspend fun edit(id: Int, content: SettingsType) {
        findById(id).setSettings(content)
    }

    suspend fun delete(id: Int) {
        val entry = entries.await()
            .getAndUpdate { list -> list.filterNot { it.controller.id == id } }
            .find { it.controller.id == id } ?: return
        entry.controller.destroy()
        entry.onDelete?.invoke(id)
    }

    suspend fun show(id: Int) { findById(id).show() }

    suspend fun hide(id: Int) { findById(id).hide() }

    private suspend fun findById(id: Int): SingleWidgetController<SettingsType, OverlayWidgetType> {
        return entries.await().value.find { it.controller.id == id }?.controller ?: throw ApiActionException("No such id")
    }
}
