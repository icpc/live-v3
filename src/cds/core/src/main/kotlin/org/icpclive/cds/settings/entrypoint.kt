package org.icpclive.cds.settings

import io.github.xn32.json5k.Json5
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import org.icpclive.util.decodeFromStreamIgnoringComments
import java.nio.file.Path

public fun interface CredentialProvider {
    public operator fun get(s: String): String?
}

public fun CDSSettings.Companion.fromFile(path: Path, credentialProvider: CredentialProvider): CDSSettings {
    val file = path.toFile()
    return when {
        !file.exists() -> throw IllegalArgumentException("File ${file.absolutePath} does not exist")
        file.name.endsWith(".properties") -> throw IllegalStateException("Properties format is not supported anymore, use settings.json instead")
        file.name.endsWith(".json") -> {
            file.inputStream().use {
                Json { serializersModule = serializersModule(credentialProvider, path) }
                    .decodeFromStreamIgnoringComments(it)
            }
        }

        file.name.endsWith(".json5") -> {
            file.inputStream().use {
                Json5 { serializersModule = serializersModule(credentialProvider, path) }
                    .decodeFromString<CDSSettings>(String(it.readAllBytes()))
            }
        }

        else -> throw IllegalArgumentException("Unknown settings file extension: ${file.path}")
    }
}