package org.icpclive.cds.settings

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromStream
import org.icpclive.cds.util.jsonForConfigs
import java.nio.file.Path

public fun interface CredentialProvider {
    public operator fun get(s: String): String?
}

private fun format(
    credentialProvider: CredentialProvider,
    path: Path,
): Json = Json(jsonForConfigs) {
    serializersModule = CDSSettings.serializersModule(credentialProvider, path)
}

public fun CDSSettings.Companion.fromFile(path: Path, credentialProvider: CredentialProvider): CDSSettings {
    val file = path.toFile()
    return if (!file.exists()) {
        throw IllegalArgumentException("File ${file.absolutePath} does not exist")
    } else {
        file.inputStream().use { format(credentialProvider, path).decodeFromStream(it) }
    }
}

public fun CDSSettings.Companion.fromContent(content: String, relativePathRoot: Path, credentialProvider: CredentialProvider): CDSSettings {
    return format(credentialProvider, relativePathRoot).decodeFromString(content)
}