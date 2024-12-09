package org.icpclive

import org.icpclive.cds.api.*
import org.icpclive.export.icpc.IcpcCsvExporter

object IcpcCSVDumpCommand : DumpFileCommand(
    name = "icpc-csv",
    help = "Dump csv for icpc.global",
    outputHelp = "Path to new csv file",
    defaultFileName = "standings.csv"
) {
    override fun format(info: ContestInfo, runs: List<RunInfo>) = IcpcCsvExporter.format(info, runs)
}