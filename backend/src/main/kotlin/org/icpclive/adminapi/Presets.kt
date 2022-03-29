package org.icpclive.adminapi

import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromStream
import kotlinx.serialization.json.encodeToStream
import org.icpclive.admin.AdminActionException
import org.icpclive.api.Preset
import org.icpclive.api.ContentPreset
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.util.concurrent.atomic.AtomicReference

class Presets<T : ContentPreset>(private val path: String,
                                 private var innerData: List<Preset<T>> = emptyList(),
                                 private var currentID: Int = innerData.size) {

    val data: List<Preset<T>>
        get() = synchronized(this) { innerData }

    private fun save(list: List<Preset<T>>) {
        try {
            innerData = list
            Json.encodeToStream(innerData.map { preset -> preset.content }, FileOutputStream(File(path)))
        } catch (e: SerializationException) {
            throw AdminActionException("Failed to serialize presets: ${e.message}")
        } catch (e: IOException) {
            throw AdminActionException("Error writing presets: ${e.message}")
        }
    }
}

inline fun <reified T : ContentPreset> Presets(path: String) = Presets(
        path,
        Json.decodeFromStream<List<T>>(FileInputStream(File(path))).mapIndexed { index, content ->
            Preset(index + 1, content)
        }
)



