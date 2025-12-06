package org.icpclive.cds.cli

import com.github.ajalt.clikt.parameters.groups.OptionGroup
import com.github.ajalt.clikt.parameters.options.*
import com.github.ajalt.clikt.parameters.types.path
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.*
import org.apache.commons.csv.CSVFormat
import org.apache.commons.csv.CSVParser
import org.icpclive.cds.ContestUpdate
import org.icpclive.cds.adapters.applyTuningRules
import org.icpclive.cds.api.PersonInfo
import org.icpclive.cds.settings.*
import org.icpclive.cds.tunning.AddPersons
import org.icpclive.cds.tunning.TuningRule
import org.icpclive.cds.util.fileContentFlow
import org.icpclive.cds.util.getLogger
import java.io.InputStream
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

    public val customFieldsCsvPath: Path by option("--custom-fields-csv", help = "Path to file with custom fields for teams")
        .path(mustExist = true, canBeFile = true, canBeDir = false)
        .defaultLazy("configDirectory/custom-fields.csv") { configDirectory.resolve("custom-fields.csv") }

    public val orgCustomFieldsCsvPath: Path by option("--org-custom-fields-csv", help = "Path to file with custom fields for organizations")
        .path(mustExist = true, canBeFile = true, canBeDir = false)
        .defaultLazy("configDirectory/org-custom-fields.csv") { configDirectory.resolve("org-custom-fields.csv") }

    public val personsJsonPath: Path by option("--persons-json", help = "Path to file with persons")
        .path(mustExist = true, canBeFile = true, canBeDir = false)
        .defaultLazy("configDirectory/persons.json") { configDirectory.resolve("persons.json")  }

    public fun toFlow(): Flow<ContestUpdate> {
        val advancedProperties = fileContentFlow(
            advancedJsonPath,
            noData = emptyList()
        ) {
            try {
                TuningRule.listFromInputStream(it)
            } catch (e: SerializationException) {
                val old = advancedJsonPath.toFile().inputStream().use { TuningRule.tryListFromLegacyFormatFromInputStream(it) } ?: throw e
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
            noData = emptyList()
        ) {
            val parsed = parseCsv(customFieldsCsvPath, it, "team_id")
            listOf(TuningRule.fromTeamFields(parsed))
        }
        val orgCustomFields = fileContentFlow(
            orgCustomFieldsCsvPath,
            noData = emptyList()
        ) {
            val parsed = parseCsv(orgCustomFieldsCsvPath, it, "org_id")
            listOf(TuningRule.fromOrganizationFields(parsed))
        }
        val persons = fileContentFlow<List<TuningRule>>(
            personsJsonPath,
            noData = emptyList()
        ) {
            val parsed = Json.decodeFromString<List<PersonInfo>>(it.reader().readText())
            listOf(AddPersons(persons = parsed))
        }

        val combinedTuningFlow = combine(customFields, orgCustomFields, persons, advancedProperties) {
            it.reduce(List<TuningRule>::plus)
        }
        log.info { "Using config directory ${this.configDirectory}" }
        log.info { "Current working directory is ${Paths.get("").toAbsolutePath()}" }
        val path = this.configDirectory.resolve("events.properties").takeIf { it.exists() }
            ?: this.configDirectory.resolve("settings.json5").takeIf { it.exists() }
            ?: this.configDirectory.resolve("settings.json")
        val creds: Map<String, String> = this.credentialFile?.let { Json.decodeFromStream<Map<String, String>?>(it.toFile().inputStream()) } ?: emptyMap()
        return CDSSettings.fromFile(path) { creds[it] }
            .toFlow()
            .applyTuningRules(combinedTuningFlow)
            .flowOn(Dispatchers.IO)
    }

    private companion object {
        val log by getLogger()
    }

    private fun parseCsv(
        path: Path,
        input: InputStream,
        idHeaderName: String,
    ): Map<String, Map<String, String>> {
        val parser = CSVParser.parse(input.reader(), CSVFormat.EXCEL.builder().setHeader().setSkipHeaderRecord(true).get())
        val names = parser.headerNames
        if (names.isEmpty()) {
            log.warning { "Ignoring malformed ${path.name}: empty file" }
            return emptyMap()
        }
        if (names[0] != idHeaderName) {
            log.warning { "Ignoring malformed ${path.name}: first column should be ${idHeaderName}" }
            return emptyMap()
        }
        return parser.records.associate { record ->
            record[0] to names.zip(record).drop(1).associate { it.first!! to it.second!! }
        }
    }

}



