package org.icpclive.export.icpc.csv

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.*
import org.apache.commons.csv.CSVFormat
import org.apache.commons.csv.CSVPrinter
import org.icpclive.cds.ContestUpdate
import org.icpclive.cds.adapters.*
import org.icpclive.cds.api.*
import org.icpclive.cds.scoreboard.getScoreboardCalculator


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

    fun TeamInfo.icpcId() = customFields["icpc_id"] ?: id.value

    fun format(info: ContestInfo, runs: List<RunInfo>) : String {
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

    fun Route.setUp(scope: CoroutineScope, contestUpdates: Flow<ContestUpdate>) {
        val stateFlow = contestUpdates
            .contestState()
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
                    state.runsAfterEvent.values.toList()
                )
            }
        }
    }
}