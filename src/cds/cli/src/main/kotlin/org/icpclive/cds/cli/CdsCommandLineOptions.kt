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
import org.icpclive.cds.api.AccountInfo
import org.icpclive.cds.api.PersonInfo
import org.icpclive.cds.settings.*
import org.icpclive.cds.tunning.AddAccounts
import org.icpclive.cds.tunning.AddPersons
import org.icpclive.cds.tunning.TuningRule
import org.icpclive.cds.util.fileContentFlow
import org.icpclive.cds.util.fileJsonContentFlow
import org.icpclive.cds.util.getLogger
import org.icpclive.cds.util.loadConfigFile
import java.io.InputStream
import java.nio.file.Path
import java.nio.file.Paths
import kotlin.io.path.exists
import kotlin.io.path.name

public open class CdsCommandLineOptions : OptionGroup("CDS options") {
    private fun configPathWithDefault(
        optionName: String,
        help: String,
        defaultName: String,
        mustExist: Boolean,
        isFile: Boolean
    ) = option(optionName, help = help)
        .path(mustExist = mustExist, canBeFile = isFile, canBeDir = !isFile)
        .defaultLazy("configDirectory/$defaultName") { configDirectory.resolve(defaultName) }

    public fun configSubDirWithDefault(
        optionName: String,
        help: String,
        defaultName: String,
        mustExist: Boolean = true,
    ): OptionWithValues<Path, Path, Path> = configPathWithDefault(optionName, help, defaultName, mustExist, isFile = false)

    public fun configFileWithDefault(
        optionName: String,
        help: String,
        defaultName: String,
        mustExist: Boolean = true,
    ): OptionWithValues<Path, Path, Path> = configPathWithDefault(optionName, help, defaultName, mustExist, isFile = true)

    public val configDirectory: Path by option(
        "-c", "--config-directory",
        help = "Path to config directory"
    ).path(mustExist = true, canBeFile = false, canBeDir = true).required()

    public val credentialFile: Path? by option(
        "--creds",
        help = "Path to file with credentials"
    ).path(mustExist = true, canBeFile = true, canBeDir = false)

    public val advancedJsonPath: Path by configFileWithDefault(
        optionName = "--advanced-json",
        help = "Path to advanced settings file",
        defaultName = "advanced-json"
    )

    public val customFieldsCsvPath: Path by configFileWithDefault(
        optionName = "--custom-fields-csv",
        help = "Path to file with custom fields for teams",
        defaultName = "custom-fields.csv"
    )

    public val orgCustomFieldsCsvPath: Path by configFileWithDefault(
        optionName = "--org-custom-fields-csv",
        help = "Path to file with custom fields for organizations",
        defaultName = "org-custom-fields.csv"
    )

    public val personsJsonPath: Path by configFileWithDefault(
        optionName = "--persons-json",
        help = "Path to file with persons",
        defaultName = "persons.json"
    )

    public val accountsJsonPath: Path by configFileWithDefault(
        optionName = "--accounts-json",
        help = "Path to file with accounts",
        defaultName = "accounts.json"
    )

    public fun toFlow(): Flow<ContestUpdate> = toFlow { }

    public fun toFlow(
       configObserver: (CDSSettings) -> Unit,
    ): Flow<ContestUpdate> {
        val advancedProperties = fileContentFlow(advancedJsonPath, noData = emptyList()) {
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
        val customFields = fileContentFlow(customFieldsCsvPath, noData = emptyList()) {
            val parsed = parseCsv(customFieldsCsvPath, it, "team_id")
            listOf(TuningRule.fromTeamFields(parsed))
        }
        val orgCustomFields = fileContentFlow(orgCustomFieldsCsvPath, noData = emptyList()) {
            val parsed = parseCsv(orgCustomFieldsCsvPath, it, "org_id")
            listOf(TuningRule.fromOrganizationFields(parsed))
        }
        val persons = fileJsonContentFlow<List<PersonInfo>>(personsJsonPath, noData = emptyList())
            .map { listOf(AddPersons(it) as TuningRule) }
        val accounts = fileJsonContentFlow<List<AccountInfo>>(accountsJsonPath, noData = emptyList())
            .map { listOf(AddAccounts(it) as TuningRule) }

        val combinedTuningFlow = combine(customFields, orgCustomFields, persons, accounts, advancedProperties) {
            it.reduce(List<TuningRule>::plus)
        }
        log.info { "Using config directory ${this.configDirectory}" }
        log.info { "Current working directory is ${Paths.get("").toAbsolutePath()}" }
        val path = this.configDirectory.resolve("events.properties").takeIf { it.exists() }
            ?: this.configDirectory.resolve("settings.json5").takeIf { it.exists() }
            ?: this.configDirectory.resolve("settings.json")
        val creds: Map<String, String> = this.credentialFile?.let { loadConfigFile<Map<String, String>>(it.toFile()) } ?: emptyMap()
        return CDSSettings.fromFile(path) { creds[it] }
            .also(configObserver)
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



