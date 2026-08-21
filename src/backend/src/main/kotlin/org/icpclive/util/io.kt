package org.icpclive.util

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.encodeToStream
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption.*

private val prettyPrintJson = Json { prettyPrint = true }

internal suspend fun <T> Path.writeJsonAtomically(serializer: KSerializer<T>, value: T): Unit =
    withContext(Dispatchers.IO) {
        val tempFile = Files.createTempFile(parent, null, null)
        try {
            tempFile.toFile().outputStream().use { file ->
                prettyPrintJson.encodeToStream(serializer, value, file)
            }
            Files.move(tempFile, this@writeJsonAtomically, REPLACE_EXISTING, ATOMIC_MOVE)
        } finally {
            Files.deleteIfExists(tempFile)
        }
    }
