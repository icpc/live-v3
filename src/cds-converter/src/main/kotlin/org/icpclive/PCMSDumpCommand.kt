package org.icpclive

import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import org.icpclive.cds.api.ContestInfo
import org.icpclive.cds.api.RunInfo
import org.icpclive.export.pcms.PCMSHtmlExporter
import org.icpclive.export.pcms.PCMSXmlExporter

object PCMSDumpCommand : DumpFileCommand(
    name = "pcms",
    help = "Dump pcms xml",
    outputHelp = "Path to new xml file",
    defaultFileName = "standings.xml"
) {
    override fun format(info: ContestInfo, runs: List<RunInfo>) = PCMSXmlExporter.format(info, runs)
}

object PCMSScoreboardDumpCommand : DumpFileCommand(
    name = "pcms-scoreboard",
    help = "Dump pcms scoreboard",
    outputHelp = "Path to new html file",
    defaultFileName = "standings.html"
) {
    override fun format(info: ContestInfo, runs: List<RunInfo>) = PCMSHtmlExporter.format(info, runs)
}