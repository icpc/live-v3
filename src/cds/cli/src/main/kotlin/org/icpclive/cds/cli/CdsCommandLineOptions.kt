package org.icpclive.cds.cli

import com.github.ajalt.clikt.parameters.groups.OptionGroup
import com.github.ajalt.clikt.parameters.options.*
import com.github.ajalt.clikt.parameters.types.path
import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromStream
import org.icpclive.cds.ContestUpdate
import org.icpclive.cds.adapters.applyAdvancedProperties
import org.icpclive.cds.settings.*
import org.icpclive.cds.tunning.AdvancedProperties
import org.icpclive.cds.util.fileJsonContentFlow
import org.icpclive.cds.util.getLogger
import org.slf4j.Logger
import java.nio.file.Path
import java.nio.file.Paths
import kotlin.io.path.exists

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

    public fun toFlow(): Flow<ContestUpdate> {
        val logger = getLogger(CdsCommandLineOptions::class)
        val advancedProperties = fileJsonContentFlow<AdvancedProperties>(advancedJsonPath, logger, AdvancedProperties())
        return CDSSettings.fromCliOptions(this, logger)
            .toFlow()
            .applyAdvancedProperties(advancedProperties)
    }
}

internal fun CDSSettings.Companion.fromCliOptions(options: CdsCommandLineOptions, log: Logger): CDSSettings {
    log.info("Using config directory ${options.configDirectory}")
    log.info("Current working directory is ${Paths.get("").toAbsolutePath()}")

    val path = options.configDirectory.resolve("events.properties")
        .takeIf { it.exists() }
        ?.also { log.warn("Using events.properties is deprecated, use settings.json instead.") }
        ?: options.configDirectory.resolve("settings.json5").takeIf { it.exists() }
        ?: options.configDirectory.resolve("settings.json")

    val creds: Map<String, String> = options.credentialFile?.let { Json.decodeFromStream(it.toFile().inputStream()) } ?: emptyMap()

    return CDSSettings.fromFile(path) { creds[it] }
}



