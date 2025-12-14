package org.icpclive.converter.commands

import org.icpclive.cds.scoreboard.ContestStateWithScoreboard
import org.icpclive.converter.export.icpc.IcpcCsvExporter

object IcpcCSVDumpCommand : DumpTextFileCommand(
    name = "icpc-csv",
    help = "Dump csv for icpc.global",
    outputHelp = "Path to new csv file",
    defaultFileName = "standings.csv"
) {
    override fun format(data: ContestStateWithScoreboard) = IcpcCsvExporter.format(data)
}