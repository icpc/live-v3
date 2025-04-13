package org.icpclive.cds.cli

import com.github.ajalt.clikt.parameters.groups.OptionGroup
import com.github.ajalt.clikt.parameters.options.*
import com.github.ajalt.clikt.parameters.types.path
import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromStream
import kotlinx.serialization.json.encodeToStream
import org.apache.commons.csv.CSVFormat
import org.apache.commons.csv.CSVParser
import org.icpclive.cds.ContestUpdate
import org.icpclive.cds.adapters.*
import org.icpclive.cds.api.toTeamId
import org.icpclive.cds.settings.*
import org.icpclive.cds.tunning.TuningRule
import org.icpclive.cds.util.*
import java.nio.file.Path
import java.nio.file.Paths
import kotlin.io.path.exists
import kotlin.io.path.name

public open class CdsCommandLineOptions : OptionGroup("CDS options") {
    public val configDirectory: Path by option(
        "-c", "--config-directory",
        help = "Path to config directory"
    ).path(mustExist = true, canBeFile = false, canBeDir = true).required()

    public val credentialFile: Path? by option(
        "--creds",
        help = "Path to file with credentials"
    ).path(mustExist = true, canBeFile = true, canBeDir = false)

    public val advancedJsonPath: Path by option("--advanced-json", help = "Path to advanced settings file")
        .path(mustExist = true, canBeFile = true, canBeDir = false)
        .defaultLazy("configDirectory/advanced.json") { configDirectory.resolve("advanced.json") }

    public val customFieldsCsvPath: Path by option("--custom-fields-csv", help = "Path to file with custom fields")
        .path(mustExist = true, canBeFile = true, canBeDir = false)
        .defaultLazy("configDirectory/custom-fields.csv") { configDirectory.resolve("custom-fields.csv") }

    public fun toFlow(): Flow<ContestUpdate> {
        val advancedProperties = fileContentFlow(
            advancedJsonPath,
            noData = emptyList()
        ) {
            try {
                TuningRule.listFromInputStream(it)
            } catch (e: SerializationException) {
                val old = advancedJsonPath.toFile().inputStream().use {  TuningRule.tryListFromLegacyFormatFromInputStream(it) } ?: throw e
                val upgradedPath = advancedJsonPath.resolveSibling(advancedJsonPath.name + ".upgraded")
                upgradedPath.toFile().outputStream().use {
                    val json = Json { prettyPrint = true }
                    json.encodeToStream(old, it)
                }
                throw SerializationException("It looks like, your ${advancedJsonPath.name} is outdated. Upgraded version is stored in ${upgradedPath.name}")
            }
        }
        val customFields = fileContentFlow(
            customFieldsCsvPath,
            noData = emptyMap()
        ) {
            val parser = CSVParser.parse(it.reader(), CSVFormat.EXCEL.builder().setHeader().setSkipHeaderRecord(true).get())
            val names = parser.headerNames
            if (names.isEmpty()) {
                log.warning { "Ignoring malformed ${customFieldsCsvPath.name}: empty file" }
                emptyMap()
            } else if (names[0] != "team_id") {
                log.warning { "Ignoring malformed ${customFieldsCsvPath.name}: first column should be team_id" }
                emptyMap()
            } else {
                parser.records.associate { record ->
                    record[0].toTeamId() to names.zip(record).drop(1).associate { it.first!! to it.second!! }
                }
            }
        }
        log.info { "Using config directory ${this.configDirectory}" }
        log.info { "Current working directory is ${Paths.get("").toAbsolutePath()}" }
        val path = this.configDirectory.resolve("events.properties")
            .takeIf { it.exists() }
            ?.also { log.warning { "Using events.properties is deprecated, use settings.json instead." } }
            ?: this.configDirectory.resolve("settings.json5").takeIf { it.exists() }
            ?: this.configDirectory.resolve("settings.json")
        val creds: Map<String, String> = this.credentialFile?.let { Json.decodeFromStream<Map<String, String>?>(it.toFile().inputStream()) } ?: emptyMap()
        return CDSSettings.fromFile(path) { creds[it] }
            .toFlow()
            .applyCustomFieldsMap(customFields)
            .applyTuningRules(advancedProperties)
    }
    private companion object {
        val log by getLogger()
    }
}



