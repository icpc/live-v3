package org.icpclive.controllers

import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.KSerializer
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.*
import kotlinx.serialization.serializer
import org.icpclive.api.ObjectSettings
import org.icpclive.api.TypeWithId
import org.icpclive.cds.util.withFallback
import org.icpclive.data.Manager
import org.icpclive.server.ApiActionException
import java.nio.file.Path
import kotlin.concurrent.atomics.*
import kotlin.time.Duration

class PresetsController<SettingsType : ObjectSettings, OverlayWidgetType : TypeWithId>(
    private val presetsPath: Path,
    private val widgetManager: Manager<OverlayWidgetType>,
    private val widgetConstructor: (SettingsType) -> OverlayWidgetType,
    settingsSerializer: KSerializer<SettingsType>,
) {
    private val fileSerializer = ListSerializer(
        WidgetState.serializer(settingsSerializer).withFallback(legacyWidgetStateSerializer(settingsSerializer))
    )
    private val mutex = Mutex()
    private val scope = CoroutineScope(Dispatchers.Default)

    private val currentID = AtomicInt(0)
    private var innerData: List<SingleWidgetController<SettingsType, OverlayWidgetType>>

    init {
        val loaded = load()
        innerData = loaded.map { it.first }
        scope.launch {
            mutex.withLock { showVisible(loaded) }
        }
    }

    suspend fun getStatus() = mutex.withLock {
        innerData.map { it.getStatus() }
    }

    suspend fun previewWidget(id: Int) = mutex.withLock {
        findById(id).previewWidget()
    }

    suspend fun createWidget(settings: SettingsType, ttl: Duration?, onDelete: suspend (Int) -> Unit = {}): Int = mutex.withLock {
        val id = currentID.incrementAndFetch()
        val wrapper = SingleWidgetController(settings, widgetManager, widgetConstructor, id, onDelete)
        innerData = innerData.plus(wrapper)
        save()
        if (ttl != null) {
            wrapper.launchWhileWidgetExists {
                delay(ttl)
                delete(id)
                // NOTHING can be done here, as coroutine is canceled by delete
            }
        }
        id
    }

    suspend fun edit(id: Int, content: SettingsType) {
        mutex.withLock {
            findById(id).setSettings(content)
            save()
        }
    }

    suspend fun delete(id: Int) {
        mutex.withLock {
            findByIdOrNull(id)?.run {
                hide()
                onDelete()
                innerData = innerData.minus(this)
                save()
            }
        }
    }

    suspend fun show(id: Int) {
        mutex.withLock {
            findById(id).show()
            save()
        }
    }

    suspend fun hide(id: Int) {
        mutex.withLock {
            findById(id).hide()
            save()
        }
    }
    suspend fun hideIfExists(id: Int) {
        mutex.withLock {
            findByIdOrNull(id)?.let {
                it.hide()
                save()
            }
        }
    }

    suspend fun reload() {
        mutex.withLock {
            for (preset in innerData) {
                preset.hide()
                preset.onDelete()
            }
            val loaded = load()
            innerData = loaded.map { it.first }
            showVisible(loaded)
        }
    }

    private fun findByIdOrNull(id: Int) = innerData.find { it.id == id }
    private fun findById(id: Int) = findByIdOrNull(id) ?: throw ApiActionException("No such id")

    private suspend fun showVisible(loaded: List<Pair<SingleWidgetController<SettingsType, OverlayWidgetType>, Boolean>>) {
        for ((preset, visible) in loaded) {
            if (visible) preset.show()
        }
    }

    private fun load(): List<Pair<SingleWidgetController<SettingsType, OverlayWidgetType>, Boolean>> = presetsPath.toFile().takeIf { it.exists() }?.inputStream()?.use {
            Json.decodeFromStream(fileSerializer, it).map { (settings, visible) ->
                SingleWidgetController(settings, widgetManager, widgetConstructor, currentID.incrementAndFetch()) to visible
            }
        } ?: emptyList()

    private suspend fun save() {
        val states = innerData.map { WidgetState(it.getSettings(), it.getStatus().shown) }
        presetsPath.writeJsonAtomically(fileSerializer, states)
    }
}

inline fun <reified SettingsType : ObjectSettings, reified OverlayWidgetType : TypeWithId> PresetsController(
    presetsPath: Path,
    widgetManager: Manager<OverlayWidgetType>,
    noinline widgetConstructor: (SettingsType) -> OverlayWidgetType
) = PresetsController(presetsPath, widgetManager, widgetConstructor, serializer())
