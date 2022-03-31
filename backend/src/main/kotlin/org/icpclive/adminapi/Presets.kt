package org.icpclive.adminapi

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromStream
import kotlinx.serialization.json.encodeToStream
import org.icpclive.admin.AdminActionException
import org.icpclive.api.Preset
import org.icpclive.api.Content
import org.icpclive.api.Widget
import org.icpclive.data.WidgetManager
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException

class Presets<ContentType : Content, WidgetType : Widget>(
        private val path: String,
        private val decode: (String) -> MutableList<Preset<ContentType>>,
        private val encode: (MutableList<Preset<ContentType>>, String) -> Unit,
        private val createWidget: (ContentType) -> WidgetType,
        private var innerData: MutableList<Preset<ContentType>> = decode(path),
        private var currentID: Int = innerData.size
) {
    private val mutex = Mutex()

    val data: List<Preset<ContentType>>
        get() = innerData

    suspend fun append(content: ContentType) {
        mutex.withLock {
            innerData.add(Preset(++currentID, content))
        }
        save()
    }

    suspend fun edit(id: Int, content: ContentType) {
        mutex.withLock {
            for (preset in innerData) {
                if (preset.id == id)
                    preset.content = content
            }
        }
        save()
    }

    suspend fun delete(id: Int) {
        mutex.withLock {
            for (preset in innerData) {
                if (preset.id != id)
                    continue
                preset.widgetId?.let {
                    WidgetManager.hideWidget(it)
                }
                preset.widgetId = null
            }
            innerData = innerData.filterNot { it.id == id }.toMutableList()
        }
        save()
    }

    suspend fun show(id: Int) = mutex.withLock {
        for (preset in innerData) {
            if (preset.id != id)
                continue
            if (preset.widgetId != null)
                continue
            val widget = createWidget(preset.content)
            WidgetManager.showWidget(widget)
            preset.widgetId = widget.widgetId
            break
        }
    }

    suspend fun hide(id: Int) = mutex.withLock {
        for (preset in innerData) {
            if (preset.id != id)
                continue
            preset.widgetId?.let {
                WidgetManager.hideWidget(it)
                println(it)
            }
            preset.widgetId = null
        }
    }

    suspend private fun load() = mutex.withLock {
        try {
            innerData = decode(path)
        } catch (e: SerializationException) {
            throw AdminActionException("Failed to deserialize presets: ${e.message}")
        } catch (e: IOException) {
            throw AdminActionException("Error reading presets: ${e.message}")
        }
    }

    suspend private fun save() = mutex.withLock {
        try {
            encode(innerData, path)
        } catch (e: SerializationException) {
            throw AdminActionException("Failed to deserialize presets: ${e.message}")
        } catch (e: IOException) {
            throw AdminActionException("Error reading presets: ${e.message}")
        }
    }
}

inline fun <reified ContentType : Content, reified WidgetType : Widget> Presets(path: String,
                                                                                noinline createWidget: (ContentType) -> WidgetType) =
        Presets<ContentType, WidgetType>(path,
                {
                    Json.decodeFromStream<List<ContentType>>(FileInputStream(File(path))).mapIndexed { index, content ->
                        Preset(index + 1, content)
                    }.toMutableList()
                },
                { data, fileName ->
                    Json { prettyPrint = true }.encodeToStream(data.map { it.content }, FileOutputStream(File(fileName)))
                },
                createWidget)