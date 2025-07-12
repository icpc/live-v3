package org.icpclive.controllers

import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.KSerializer
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.*
import kotlinx.serialization.serializer
import org.icpclive.admin.ApiActionException
import org.icpclive.api.ObjectSettings
import org.icpclive.api.TypeWithId
import org.icpclive.data.Manager
import java.nio.file.Files
import java.nio.file.Path
import java.util.concurrent.atomic.*
import kotlin.time.Duration

class PresetsController<SettingsType : ObjectSettings, OverlayWidgetType : TypeWithId>(
    private val presetsPath: Path,
    private val widgetManager: Manager<OverlayWidgetType>,
    private val widgetConstructor: (SettingsType) -> OverlayWidgetType,
    settingsSerializer: KSerializer<SettingsType>,
) {
    private val fileSerializer = ListSerializer(settingsSerializer)
    private val mutex = Mutex()

    private val currentID = AtomicInteger(0)
    private var innerData = load()

    suspend fun getStatus() = mutex.withLock {
        innerData.map { it.getStatus() }
    }

    suspend fun previewWidget(id: Int) = mutex.withLock {
        findById(id).previewWidget()
    }

    suspend fun createWidget(settings: SettingsType, ttl: Duration?, onDelete: suspend (Int) -> Unit = {}): Int = mutex.withLock {
        val id = currentID.incrementAndGet()
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
        }
    }

    suspend fun hide(id: Int) {
        mutex.withLock {
            findById(id).hide()
        }
    }
    suspend fun hideIfExists(id: Int) {
        mutex.withLock {
            findByIdOrNull(id)?.hide()
        }
    }

    suspend fun reload() {
        mutex.withLock {
            for (preset in innerData) {
                preset.hide()
                preset.onDelete()
            }
            innerData = load()
        }
    }

    private fun findByIdOrNull(id: Int) = innerData.find { it.id == id }
    private fun findById(id: Int) = findByIdOrNull(id) ?: throw ApiActionException("No such id")

    private fun load() = presetsPath.toFile().takeIf { it.exists() }?.inputStream()?.use {
            Json.decodeFromStream(fileSerializer, it).map { content ->
                SingleWidgetController(content, widgetManager, widgetConstructor, currentID.incrementAndGet())
            }
        } ?: emptyList()

    private suspend fun save(): Unit = withContext(Dispatchers.IO) {
        val tempFile = Files.createTempFile(presetsPath.parent, null, null)
        tempFile.toFile().outputStream().use { file ->
            jsonPrettyEncoder.encodeToStream(
                fileSerializer,
                innerData.map { it.getSettings() },
                file
            )
        }
        Files.deleteIfExists(presetsPath)
        Files.move(tempFile, presetsPath)
    }

    companion object {
        private val jsonPrettyEncoder = Json { prettyPrint = true }
    }
}

inline fun <reified SettingsType : ObjectSettings, reified OverlayWidgetType : TypeWithId> PresetsController(
    presetsPath: Path,
    widgetManager: Manager<OverlayWidgetType>,
    noinline widgetConstructor: (SettingsType) -> OverlayWidgetType
) = PresetsController(presetsPath, widgetManager, widgetConstructor, serializer())
