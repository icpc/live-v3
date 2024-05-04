package org.icpclive.cds.settings

import io.github.xn32.json5k.Json5
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.*
import java.io.InputStream
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

private inline fun <reified T> Json.decodeFromStreamIgnoringComments(stream: InputStream) : T = decodeFromJsonElement(decodeFromStream<JsonElement>(stream).cleanFromComments())

private fun JsonElement.cleanFromComments() : JsonElement {
    return when (this) {
        is JsonArray -> JsonArray(map { it.cleanFromComments() })
        is JsonObject -> JsonObject(filter { !it.key.startsWith("#") }.mapValues { it.value.cleanFromComments() })
        is JsonPrimitive, JsonNull -> this
    }
}
