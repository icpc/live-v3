package org.icpclive.admin

import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromStream
import java.io.File
import java.io.FileInputStream
import java.io.IOException
import java.util.concurrent.atomic.AtomicReference

class Presets<T>(private val path: String, private val decode: (String) -> List<T>) {
    var data: AtomicReference<List<T>> = AtomicReference(emptyList())

    init {
        reload()
    }

    fun reload() {
        try {
            data.set(decode(path))
        } catch (e: SerializationException) {
            throw AdminActionException("Failed to deserialize presets: ${e.message}")
        } catch (e: IOException) {
            throw AdminActionException("Error reading presets: ${e.message}")
        }
    }
}

inline fun <reified T> Presets(path: String) = Presets<T>(path) {
    Json.decodeFromStream(FileInputStream(File(it)))
}