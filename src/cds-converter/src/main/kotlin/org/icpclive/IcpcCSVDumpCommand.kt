package org.icpclive

import org.icpclive.cds.api.*
import org.icpclive.cds.scoreboard.ContestStateWithScoreboard
import org.icpclive.export.icpc.IcpcCsvExporter

object IcpcCSVDumpCommand : DumpFileCommand(
    name = "icpc-csv",
    help = "Dump csv for icpc.global",
    outputHelp = "Path to new csv file",
    defaultFileName = "standings.csv"
) {
    override fun format(data: ContestStateWithScoreboard) = IcpcCsvExporter.format(data)
}