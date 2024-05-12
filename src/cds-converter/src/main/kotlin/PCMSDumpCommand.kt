package org.icpclive

import org.icpclive.cds.api.ContestInfo
import org.icpclive.cds.api.RunInfo
import org.icpclive.org.icpclive.export.pcms.PCMSExporter

object PCMSDumpCommand : DumpFileCommand(
    name = "pcms",
    help = "Dump pcms xml",
    outputHelp = "Path to new xml file",
    defaultFileName = "standings.xml"
) {
    override fun format(info: ContestInfo, runs: List<RunInfo>) = PCMSExporter.format(info, runs)
}