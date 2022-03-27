package org.icpclive.adminapi

import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromStream
import kotlinx.serialization.json.encodeToStream
import org.icpclive.admin.AdminActionException
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.util.concurrent.atomic.AtomicReference

class Presets<T>(
    private val path: String,
    private val decode: (String) -> List<T>,
    private val encode: (List<T>, String) -> Unit
) {
    var data: List<T>
        get() = synchronized(this) { load() }
        set(data) = synchronized(this) { save(data) }


    private fun load(): List<T> {
        try {
            return decode(path)
        } catch (e: SerializationException) {
            throw AdminActionException("Failed to deserialize presets: ${e.message}")
        } catch (e: IOException) {
            throw AdminActionException("Error reading presets: ${e.message}")
        }
    }

    private fun save(list: List<T>) {
        try {
            encode(list, path)
        } catch (e: SerializationException) {
            throw AdminActionException("Failed to deserialize presets: ${e.message}")
        } catch (e: IOException) {
            throw AdminActionException("Error reading presets: ${e.message}")
        }
    }
}

inline fun <reified T> Presets(presetsType: String) = Presets<T>(presetsType, {
    Json.decodeFromStream(FileInputStream(File(it)))
}, { data, fileName ->
    Json.encodeToStream(data, FileOutputStream(File(fileName)))
})
