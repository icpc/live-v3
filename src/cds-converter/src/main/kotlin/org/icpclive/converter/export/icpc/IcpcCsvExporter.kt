package org.icpclive.converter.export.icpc

import io.ktor.http.*
import org.apache.commons.csv.CSVFormat
import org.apache.commons.csv.CSVPrinter
import org.icpclive.cds.api.*
import org.icpclive.cds.scoreboard.ContestStateWithScoreboard
import org.icpclive.converter.export.SingleFileExporter


object IcpcCsvExporter : SingleFileExporter(
    httpPath = "/icpc",
    exportName = "standings.csv",
    exportDescription = "icpc.global csv",
    contentType = ContentType.Text.CSV
) {
    val fields = listOf(
        "teamId",
        "rank",
        "medalCitation",
        "problemsSolved",
        "totalTime",
        "lastProblemTime",
        "siteCitation",
        "citation",
        "teamName",
        "institution"
    )

    fun TeamInfo.icpcId() = customFields["icpc_id"] ?: id.value

    override fun format(state: ContestStateWithScoreboard): String {
        val info = state.state.infoAfterEvent!!
        if (info.resultType == ContestResultType.IOI) TODO("IOI is not supported yet")
        val ranking = state.rankingAfter
        val ranks = ranking.order.zip(ranking.ranks).toMap()
        val icpcTeamIds = info.teams.values.associate { it.id to it.icpcId() }

        return buildString {
            val printer = CSVPrinter(this, CSVFormat.DEFAULT.builder().setHeader(*fields.toTypedArray()).get())
            for (teamId in ranking.order.reversed()) {
                printer.printRecord(getFields(icpcTeamIds[teamId]!!, ranks[teamId]!!, state.scoreboardRowAfter(teamId), ranking.awards.filter { teamId in it.teams }))
            }
        }
    }

    private fun getFields(icpc_id: String, rank: Int, scoreboardRow: ScoreboardRow, awards: List<Award>): List<String> {
        fun award(substring: String) = awards.filter { it.id.contains(substring) }.also { require(it.size < 2) }.singleOrNull()
        return listOf(
            icpc_id,
            rank.toString(),
            (award("disqualified") ?: award("-medal"))?.citation ?: "",
            scoreboardRow.totalScore.toInt().toString(),
            scoreboardRow.penalty.inWholeMinutes.toString(),
            scoreboardRow.problemResults.filter { it is ICPCProblemResult && it.isSolved }.maxOfOrNull { it.lastSubmitTime!!.inWholeMinutes }?.toString() ?: "0",
            (award("disqualified") ?: award("winner") ?: award("-diploma"))?.citation ?: "",
            (award("disqualified") ?: award("rank-"))?.citation ?: "Honorable Mention",
            "",
            ""
        )
    }
}
