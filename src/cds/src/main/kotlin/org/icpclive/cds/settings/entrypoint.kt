package org.icpclive.cds.settings

import com.github.ajalt.clikt.parameters.groups.OptionGroup
import com.github.ajalt.clikt.parameters.options.*
import com.github.ajalt.clikt.parameters.types.path
import io.github.xn32.json5k.Json5
import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromStream
import org.icpclive.api.tunning.AdvancedProperties
import org.icpclive.cds.ContestUpdate
import org.icpclive.cds.adapters.*
import org.icpclive.util.decodeFromStreamIgnoringComments
import org.icpclive.util.fileJsonContentFlow
import org.slf4j.Logger
import java.nio.file.Path
import java.nio.file.Paths
import kotlin.io.path.exists

public fun interface CredentialProvider {
    public operator fun get(s: String) : String?
}

public fun parseFileToCdsSettings(path: Path, credentialProvider: CredentialProvider) : CDSSettings {
    val file = path.toFile()
    return when {
        !file.exists() -> throw IllegalArgumentException("File ${file.absolutePath} does not exist")
        file.name.endsWith(".properties") -> throw IllegalStateException("Properties format is not supported anymore, use settings.json instead")
        file.name.endsWith(".json") -> {
            file.inputStream().use {
                Json { serializersModule = CDSSettings.serializersModule(credentialProvider, path) }.decodeFromStreamIgnoringComments(it)
            }
        }
        file.name.endsWith(".json5") -> {
            file.inputStream().use {
                Json5 { serializersModule = CDSSettings.serializersModule(credentialProvider, path) }.decodeFromString<CDSSettings>(String(it.readAllBytes()))
            }
        }
        else -> throw IllegalArgumentException("Unknown settings file extension: ${file.path}")
    }
}

public class CdsCommandLineOptions : OptionGroup("CDS options") {
    public val configDirectory: Path by option(
        "-c", "--config-directory",
        help = "Path to config directory"
    ).path(mustExist = true, canBeFile = false, canBeDir = true).required()
    public val credentialFile: Path? by option(
        "--creds",
        help = "Path to file with credentials"
    ).path(mustExist = true, canBeFile = true, canBeDir = false)
    public val advancedJsonPath: Path by option("--advanced-json", help = "Path to advanced.json")
        .path(mustExist = true, canBeFile = true, canBeDir = false)
        .defaultLazy("configDirectory/advanced.json") { configDirectory.resolve("advanced.json") }
}

@OptIn(ExperimentalSerializationApi::class)
public fun CdsCommandLineOptions.toFlow(log: Logger) : Flow<ContestUpdate> {
    log.info("Using config directory ${configDirectory}")
    log.info("Current working directory is ${Paths.get("").toAbsolutePath()}")
    val path = configDirectory.resolve("events.properties")
        .takeIf { it.exists() }
        ?.also { log.warn("Using events.properties is deprecated, use settings.json instead.") }
        ?: configDirectory.resolve("settings.json5").takeIf { it.exists() }
        ?: configDirectory.resolve("settings.json")
    val creds: Map<String, String> = credentialFile?.let {
        Json.decodeFromStream(it.toFile().inputStream())
    } ?: emptyMap()
    val advancedProperties = fileJsonContentFlow<AdvancedProperties>(advancedJsonPath, log, AdvancedProperties())

    return parseFileToCdsSettings(path) { creds[it] }
        .toFlow()
        .applyAdvancedProperties(advancedProperties)

}
