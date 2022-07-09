package org.icpclive.admin

import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.KSerializer
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromStream
import kotlinx.serialization.json.encodeToStream
import kotlinx.serialization.serializer
import org.icpclive.api.ObjectSettings
import org.icpclive.api.ObjectStatus
import org.icpclive.api.TypeWithId
import org.icpclive.data.Manager
import java.io.FileInputStream
import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.nio.file.Files
import java.nio.file.Path

private val jsonPrettyEncoder = Json { prettyPrint = true }
private const val DEFAULT_TTL = 3000L

class PresetsManager<SettingsType : ObjectSettings, ItemType : TypeWithId>(
    private val path: Path,
    settingsSerializer: KSerializer<SettingsType>,
    private val createItem: (SettingsType) -> ItemType,
    private val manager: Manager<ItemType>,
) {
    private val mutex = Mutex()
    private val serializer = ListSerializer(settingsSerializer)
    private var innerData = load()
    private var currentID = innerData.size

    suspend fun getStatus(): List<ObjectStatus<SettingsType>> = mutex.withLock {
        return innerData.map { it.getStatus() }
    }

    suspend fun append(settings: SettingsType): Int {
        var id: Int
        mutex.withLock {
            id = ++currentID
            innerData = innerData.plus(Wrapper(createItem, settings, manager, id))
        }
        save()
        return id
    }

    suspend fun edit(id: Int, content: SettingsType) {
        mutex.withLock {
            for (preset in innerData) {
                if (preset.id == id)
                    preset.set(content)
            }
        }
        save()
    }

    suspend fun delete(id: Int) {
        mutex.withLock {
            for (preset in innerData) {
                if (preset.id != id)
                    continue
                preset.hide()
            }
            innerData = innerData.filterNot { it.id == id }
        }
        save()
    }

    suspend fun show(id: Int) {
        mutex.withLock {
            for (preset in innerData) {
                if (preset.id != id)
                    continue
                preset.show()
                break
            }
        }
    }

    //TODO: rework
    @OptIn(DelicateCoroutinesApi::class)
    suspend fun showWithTTL(settings: SettingsType, ttl: Long?) {
        val id = append(settings)
        show(id)
        GlobalScope.launch {
            delay(ttl ?: DEFAULT_TTL)
            delete(id)
        }
    }

    suspend fun hide(id: Int) {
        mutex.withLock {
            for (preset in innerData) {
                if (preset.id != id)
                    continue
                preset.hide()
            }
        }
    }

    suspend fun reload() {
        mutex.withLock {
            for (preset in innerData) {
                preset.hide()
            }
            innerData = load()
        }
    }

    private fun load() = try {
        FileInputStream(path.toFile()).use {
            Json.decodeFromStream(serializer, it).mapIndexed { index, content ->
                Wrapper(createItem, content, manager, index + 1)
            }
        }
    } catch (e: FileNotFoundException) {
        emptyList()
    }

    private suspend fun save() {
        mutex.withLock {
            val tempFile = Files.createTempFile(path.parent, null, null)
            FileOutputStream(tempFile.toFile()).use { file ->
                jsonPrettyEncoder.encodeToStream(
                    serializer,
                    innerData.map { it.getSettings() },
                    file
                )
            }
            Files.deleteIfExists(path)
            Files.move(tempFile, path)
        }
    }
}


inline fun <reified SettingsType : ObjectSettings, reified ItemType : TypeWithId> Presets(
    path: Path,
    manager: Manager<ItemType>,
    noinline createItem: (SettingsType) -> ItemType
) = PresetsManager(
    path,
    serializer(),
    createItem,
    manager
)