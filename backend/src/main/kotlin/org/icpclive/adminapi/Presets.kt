package org.icpclive.adminapi

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromStream
import kotlinx.serialization.json.encodeToStream
import org.icpclive.api.*
import org.icpclive.data.Manager
import org.icpclive.data.TickerManager
import org.icpclive.data.WidgetManager
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream

class PresetsManager<SettingsType : ObjectSettings, WidgetType : TypeWithId>(
    private val path: String,
    private val decode: (String) -> List<Wrapper<SettingsType, WidgetType>>,
    private val encode: (List<Wrapper<SettingsType, WidgetType>>, String) -> Unit,
    private val createWidget: (SettingsType) -> WidgetType,
    private val manager: Manager<WidgetType>,
    private var innerData: List<Wrapper<SettingsType, WidgetType>> = decode(path),
    private var currentID: Int = innerData.size
) {
    private val mutex = Mutex()

    suspend fun getStatus(): List<ObjectStatus<SettingsType>> = mutex.withLock {
        return innerData.map { it.getStatus() }
    }

    suspend fun append(settings: SettingsType) {
        mutex.withLock {
            innerData = innerData.plus(Wrapper(createWidget, settings, manager, ++currentID))
        }
        save()
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

    suspend fun hide(id: Int) {
        mutex.withLock {
            for (preset in innerData) {
                if (preset.id != id)
                    continue
                preset.hide()
            }
        }
    }

    private suspend fun load() {
        mutex.withLock {
            innerData = decode(path)
        }
    }

    private suspend fun save() {
        mutex.withLock {
            encode(innerData, path)
        }
    }
}

val jsonPrettyEncoder = Json { prettyPrint = true }

inline fun <reified SettingsType : ObjectSettings, reified WidgetType : Widget> widgetPresets(
    path: String,
    noinline createWidget: (SettingsType) -> WidgetType
) =
    PresetsManager(
        path,
        {
            Json.decodeFromStream<List<SettingsType>>(FileInputStream(File(path))).mapIndexed { index, content ->
                Wrapper(createWidget, content, WidgetManager, index + 1)
            }
        },
        { data, fileName ->
            jsonPrettyEncoder.encodeToStream(data.map { it.getSettings() }, FileOutputStream(File(fileName)))
        },
        createWidget,
        WidgetManager
    )

fun tickerPresets(
    path: String,
    createMessage: (TickerMessageSettings) -> TickerMessage
) =
    PresetsManager(
        path,
        {
            Json.decodeFromStream<List<TickerMessageSettings>>(FileInputStream(File(it))).mapIndexed { index, content ->
                Wrapper(createMessage, content, TickerManager, index + 1)
            }
        },
        { data, fileName ->
            jsonPrettyEncoder.encodeToStream(data.map { it.getSettings() }, FileOutputStream(File(fileName)))
        },
        createMessage,
        TickerManager
    )