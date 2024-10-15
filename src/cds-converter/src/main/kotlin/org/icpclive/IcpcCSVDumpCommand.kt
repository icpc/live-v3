package org.icpclive

import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.types.file
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import org.apache.commons.csv.CSVFormat
import org.apache.commons.csv.CSVParser
import org.icpclive.cds.ContestUpdate
import org.icpclive.cds.adapters.applyAdvancedProperties
import org.icpclive.cds.api.*
import org.icpclive.cds.tunning.AdvancedProperties
import org.icpclive.cds.tunning.TeamInfoOverride
import org.icpclive.export.icpc.IcpcCsvExporter

object IcpcCSVDumpCommand : DumpFileCommand(
    name = "icpc-csv",
    help = "Dump csv for icpc.global",
    outputHelp = "Path to new csv file",
    defaultFileName = "standings.csv"
) {
    val teamsMapping by option("--teams-map", help = "mapping from cds team id to icpc team id")
        .file(canBeFile = true, canBeDir = false, mustExist = true)

    override fun format(info: ContestInfo, runs: List<RunInfo>) = IcpcCsvExporter.format(info, runs)
    override fun Flow<ContestUpdate>.postprocess(): Flow<ContestUpdate> {
        val mappingFile = teamsMapping
        if (mappingFile == null) {
            return this
        } else {
            val parser = CSVParser(mappingFile.inputStream().reader(), CSVFormat.TDF)
            val map = parser.records.associate {
                it[1]!!.toTeamId() to it[0]!!
            }
            val advanced = AdvancedProperties(
                teamOverrides = map.mapValues {
                    TeamInfoOverride(
                        customFields = mapOf("icpc_id" to it.value)
                    )
                }
            )
            return applyAdvancedProperties(flow { emit(advanced) })
        }
    }
}