package org.icpclive.cds.settings

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromStream
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
            val format = Json {
                serializersModule = serializersModule(credentialProvider, path)
                allowTrailingComma = true
                allowComments = true
            }
            file.inputStream().use { format.decodeFromStream(it) }
        }
        else -> throw IllegalArgumentException("Unknown settings file extension: ${file.path}")
    }
}