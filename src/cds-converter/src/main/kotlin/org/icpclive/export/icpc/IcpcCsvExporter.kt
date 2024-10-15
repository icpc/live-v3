package org.icpclive.export.icpc

import io.ktor.http.*
import org.apache.commons.csv.CSVFormat
import org.apache.commons.csv.CSVPrinter
import org.icpclive.cds.api.*
import org.icpclive.cds.scoreboard.getScoreboardCalculator
import org.icpclive.export.SingleFileExporter


object IcpcCsvExporter : SingleFileExporter("standings.csv", ContentType.Text.CSV) {
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

    override fun format(info: ContestInfo, runs: List<RunInfo>): String {
        if (info.resultType == ContestResultType.IOI) TODO("IOI is not supported yet")
        val runsByTeam = runs.groupBy { it.teamId }
        val scoreboardCalculator = getScoreboardCalculator(info, OptimismLevel.NORMAL)
        val rows = info.teams.keys.associateWith { scoreboardCalculator.getScoreboardRow(info, runsByTeam[it] ?: emptyList()) }
        val ranking = scoreboardCalculator.getRanking(info, rows)
        val ranks = ranking.order.zip(ranking.ranks).toMap()
        val icpcTeamIds = info.teams.values.associate { it.id to it.icpcId() }

        return buildString {
            val printer = CSVPrinter(this, CSVFormat.DEFAULT.builder().setHeader(*fields.toTypedArray()).build())
            for (teamId in ranking.order.reversed()) {
                printer.printRecord(getFields(icpcTeamIds[teamId]!!, ranks[teamId]!!, rows[teamId]!!, ranking.awards.filter { teamId in it.teams }))
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
