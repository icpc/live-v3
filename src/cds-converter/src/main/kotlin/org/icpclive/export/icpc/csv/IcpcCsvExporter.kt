package org.icpclive.org.icpclive.export.pcms

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.*
import org.apache.commons.csv.CSVFormat
import org.apache.commons.csv.CSVPrinter
import org.icpclive.api.*
import org.icpclive.cds.ContestUpdate
import org.icpclive.cds.adapters.*
import org.icpclive.scoreboard.getScoreboardCalculator
import org.icpclive.util.atMostOne


object IcpcCsvExporter {
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

    fun TeamInfo.icpcId() = customFields["icpc_id"] ?: contestSystemId

    fun format(info: ContestInfo, runsByTeam: Map<Int, List<RunInfo>>) : String {
        if (info.resultType == ContestResultType.IOI) TODO("IOI is not supported yet")
        val scoreboardCalculator = getScoreboardCalculator(info, OptimismLevel.NORMAL)
        val rows = info.teams.keys.associateWith { scoreboardCalculator.getScoreboardRow(info, runsByTeam[it] ?: emptyList()) }
        val ranking = scoreboardCalculator.getRanking(info, rows)
        val ranks = ranking.order.zip(ranking.ranks).toMap()
        val icpcTeamIds = info.teams.values.map { it.icpcId() to it.id }.sortedByDescending { ranks[it.second] }

        return buildString {
            val printer = CSVPrinter(this, CSVFormat.DEFAULT.builder().setHeader(*fields.toTypedArray()).build())
            for ((icpc_id, team_id) in icpcTeamIds) {
                printer.printRecord(getFields(icpc_id, ranks[team_id]!!, rows[team_id]!!, ranking.awards.filter { team_id in it.teams }))
            }
        }
    }

    private fun getFields(icpc_id: String, rank: Int, scoreboardRow: ScoreboardRow, awards: List<Award>): List<String> {
        fun award(substring: String) = awards.atMostOne { it.id.contains(substring) }
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

    fun Route.setUp(scope: CoroutineScope, contestUpdates: Flow<ContestUpdate>) {
        val stateFlow = contestUpdates
            .stateGroupedByTeam()
            .stateIn(scope, SharingStarted.Eagerly, null)
            .filterNotNull()
            .filter { it.infoAfterEvent != null }
        get {
            call.respondRedirect("/icpc/standings.csv", permanent = true)
        }
        get("standings.csv") {
            call.respondText(contentType = ContentType.Text.CSV) {
                val state = stateFlow.first()
                format(
                    state.infoAfterEvent!!,
                    state.runs
                )
            }
        }
    }
}