package org.icpclive.adminapi

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.json.*
import org.icpclive.api.*
import org.icpclive.data.TickerManager
import org.icpclive.data.WidgetManager
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException

// TODO: Unify with PresetManager
/*
class TickerPresetsManager(
        private val path: String,
        private val decode: (String) -> List<Pair<TickerMessage, Int>>,
        private val encode: (List<Pair<TickerMessage, Int>>, String) -> Unit,
        private var innerData: List<WidgetWrapper<SettingsType, WidgetType>> = decode(path),
        private var currentID: Int = innerData.size
) {
    private val mutex = Mutex()

    suspend fun getStatus(): List<ObjectStatus<SettingsType>> = mutex.withLock {
        return innerData.map { it.getStatus() }
    }

    suspend fun append(settings: SettingsType) {
        mutex.withLock {
            innerData = innerData.plus(WidgetWrapper(createWidget, settings, ++currentID))
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

    suspend private fun load() {
        mutex.withLock {
            innerData = decode(path)
        }
    }

    suspend private fun save() {
        mutex.withLock {
            encode(innerData, path)
        }
    }
}

val jsonPrettyEncoder = Json { prettyPrint = true }

inline fun <reified SettingsType : ObjectSettings, reified WidgetType : Widget> Presets(path: String,
                                                                                        noinline createWidget: (SettingsType) -> WidgetType) =
        PresetsManager<SettingsType, WidgetType>(path,
                {
                    Json.decodeFromStream<List<SettingsType>>(FileInputStream(File(path))).mapIndexed { index, content ->
                        WidgetWrapper(createWidget, content, index + 1)
                    }
                },
                { data, fileName ->
                    Json { prettyPrint = true }.encodeToStream(data.map { it.settings }, FileOutputStream(File(fileName)))
                },
                createWidget)
*/