package org.icpclive.controllers

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.*
import org.icpclive.api.ObjectSettings
import org.icpclive.cds.util.map
import java.nio.file.Files
import java.nio.file.Path

@Serializable
data class WidgetState<SettingsType : ObjectSettings>(
    val settings: SettingsType,
    val visible: Boolean = false,
)

fun <SettingsType : ObjectSettings> legacyWidgetStateSerializer(
    settingsSerializer: KSerializer<SettingsType>,
): KSerializer<WidgetState<SettingsType>> = settingsSerializer.map(
    "LegacyWidgetState<${settingsSerializer.descriptor.serialName}>",
    { WidgetState(it) },
    { it.settings }
)

internal val widgetStateJson = Json { prettyPrint = true }

internal suspend fun <T> Path.writeJsonAtomically(serializer: KSerializer<T>, value: T): Unit =
    withContext(Dispatchers.IO) {
        val tempFile = Files.createTempFile(parent, null, null)
        tempFile.toFile().outputStream().use { file ->
            widgetStateJson.encodeToStream(serializer, value, file)
        }
        Files.deleteIfExists(this@writeJsonAtomically)
        Files.move(tempFile, this@writeJsonAtomically)
    }
