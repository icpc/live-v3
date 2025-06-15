package org.icpclive

import org.icpclive.cds.scoreboard.ContestStateWithScoreboard
import org.icpclive.export.pcms.PCMSHtmlExporter
import org.icpclive.export.pcms.PCMSXmlExporter

object PCMSDumpCommand : DumpFileCommand(
    name = "pcms",
    help = "Dump pcms xml",
    outputHelp = "Path to new xml file",
    defaultFileName = "standings.xml"
) {
    override fun format(data: ContestStateWithScoreboard) = PCMSXmlExporter.format(data)
}

object PCMSScoreboardDumpCommand : DumpFileCommand(
    name = "pcms-scoreboard",
    help = "Dump pcms scoreboard",
    outputHelp = "Path to new html file",
    defaultFileName = "standings.html"
) {
    override fun format(data: ContestStateWithScoreboard) = PCMSHtmlExporter.format(data)
}