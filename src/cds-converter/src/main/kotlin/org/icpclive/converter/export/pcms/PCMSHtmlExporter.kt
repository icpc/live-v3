package org.icpclive.converter.export.pcms

import io.ktor.http.*
import kotlinx.html.*
import kotlinx.html.stream.appendHTML
import org.icpclive.cds.api.*
import org.icpclive.cds.scoreboard.ContestStateWithScoreboard
import org.icpclive.converter.export.SingleFileExporter
import org.icpclive.converter.export.pcms.PCMSXmlExporter.toPcmsStatus
import kotlin.time.Duration

object PCMSHtmlExporter : SingleFileExporter("pcms","standings.html", ContentType.Text.Html) {
    private fun HTML.htmlHeader() {
        head {
            meta {
                httpEquiv = "Content-Type"
                content = "text/html; charset=utf-8"
            }
            title("Standings")
            styleLink("https://nerc.itmo.ru/standings2.css")
            script {
                type = "text/javascript"
                unsafe {
                    +"""
                        function onShowTime() {
                            var showTime = document.getElementById("show-time");
                            var display = showTime.checked ? "" : "none";
        
                            var times = document.getElementsByTagName('s');
                            for (var i = 0; i < times.length; i++){
                                times[i].style.display = display;
                            }    
                        }
                    """.trimIndent()
                }
            }
        }
    }

    var awardGroupsEnabled: List<Int> = emptyList()

    private fun TD.pageHeader(info: ContestInfo) {
        fun Duration.format() = toComponents { hours, minutes, seconds, _ ->
            "%d:%02d:%02d".format(hours, minutes, seconds)
        }
        a {
            attributes["name"] = info.name
            h2 { +info.name }
        }
        p {
            +"${info.currentContestTime.format()} of ${info.contestLength.format()}"
            br
            +"status: ${info.status.toPcmsStatus()}"
        }
        p {
            input(type = InputType.checkBox, name = "show-times") {
                onClick = "onShowTime()"
                id = "show-time"
                checked = true
            }
            +"Show time"
        }
    }

    private fun Duration.formatTime() = toComponents { minutes, seconds, _ ->
        "%d:%02d".format(minutes, seconds)
    }

    private fun TABLE.tableHeader(info: ContestInfo, awards: List<Award>) = thead {
        tr {
            th(classes = "rankl") { +"Rank" }
            th(classes = "party") { +"Team" }
            for (problem in info.scoreboardProblems) {
                th(classes = "problem") {
                    title = problem.fullName
                    +problem.displayName
                }
            }
            th(classes = "solved") { +"=" }
            th(classes = "penalty") { +"Time" }
            awardGroupsEnabled = KnownAwards.entries
                .filter { a -> awards.any { it.id == a.awardId } }
                .map { it.awardGroup }
                .distinct()
                .sorted()
            if (awardGroupsEnabled.isNotEmpty()) {
                th(classes = "rank") {
                    colSpan = awardGroupsEnabled.size.toString()
                    +"Awards"
                }
            }
        }
    }

    private fun TABLE.tableFooter(okRuns: List<List<RunInfo>>, nokRuns: List<List<RunInfo>>) = tfoot {
        fun row(header: String, block: (List<RunInfo>, List<RunInfo>) -> String) = tr {
            td {}
            td { +header }
            for ((r1, r2) in okRuns.zip(nokRuns)) {
                td { +block(r1, r2) }
            }
        }
        row("Total runs") { r1, r2 -> (r1.count() + r2.count()).toString() }
        row("Accepted") { r1, _ -> r1.count().toString() }
        row("Rejected") { _, r2 -> r2.count().toString() }
        row("First Accept") { r1, _ -> r1.minOfOrNull { it.time }?.formatTime().orEmpty() }
        row("Last Accept") { r1, _ -> r1.maxOfOrNull { it.time }?.formatTime().orEmpty() }
    }

    private fun TR.teamRow(info: TeamInfo, r: ScoreboardRow, rank: Int, awards: List<Award>) {
        td("rankl") { +rank.toString() }
        td("party") { +info.displayName }
        for (probResult in r.problemResults) {
            td {
                probResult as ICPCProblemResult
                when {
                    probResult.isSolved -> i {
                        if (probResult.isFirstToSolve) {
                            classes += "first-to-solve"
                        }
                        +"+"
                        if (probResult.wrongAttempts > 0) {
                            +probResult.wrongAttempts.toString()
                        }
                        s {
                            br
                            +probResult.lastSubmitTime!!.formatTime()
                        }
                    }

                    probResult.pendingAttempts > 0 -> em { +"?${probResult.wrongAttempts + probResult.pendingAttempts}" }
                    probResult.wrongAttempts > 0 -> b { +"-${probResult.wrongAttempts}" }
                }
            }
        }
        td { +r.totalScore.toInt().toString() }
        td("penalty") { +r.penalty.inWholeMinutes.toString() }
        val gr = awards.mapNotNull { award ->
            KnownAwards.entries.singleOrNull { it.awardId == award.id }
        }.groupBy { it.awardGroup }
        for (g in awardGroupsEnabled) {
            val m = gr[g].orEmpty()
            td("rank") {
                m.forEach {
                    span(it.style) { +it.code }
                }
            }
        }
    }



    override fun format(state: ContestStateWithScoreboard): String {
        val info = state.state.infoAfterEvent!!
        val ranking = state.rankingAfter
        val runsByProblem = state.state.runsAfterEvent.values.groupBy { it.problemId }
        if (info.resultType == ContestResultType.IOI) TODO("IOI is not supported yet")


        return buildString {
            appendHTML(prettyPrint = true).html {
                htmlHeader()
                body {
                    table("wrapper") {
                        tr {
                            td {
                                attributes["text-align"] = "center"
                                pageHeader(info)
                                table("standings") {
                                    tableHeader(info, ranking.awards)
                                    tbody {
                                        var currentProblemNum = Double.NaN
                                        var problemColorParity = 1
                                        var rowParity = 0
                                        for ((teamId, rank) in ranking.order.zip(ranking.ranks)) {
                                            val r = state.scoreboardRowAfter(teamId)
                                            rowParity = 1 - rowParity
                                            if (currentProblemNum != r.totalScore) {
                                                currentProblemNum = r.totalScore
                                                problemColorParity = 1 - problemColorParity
                                            }
                                            tr("row${problemColorParity}${rowParity}") {
                                                teamRow(info.teams[teamId]!!, r, rank, ranking.awards.filter { teamId in it.teams })
                                            }
                                        }
                                    }
                                    tableFooter(
                                        info.scoreboardProblems.map { runsByProblem[it.id]?.filter { it.teamId in ranking.order && ((it.result as? RunResult.ICPC)?.verdict?.isAccepted == true) } ?: emptyList() },
                                        info.scoreboardProblems.map { runsByProblem[it.id]?.filter { it.teamId in ranking.order && ((it.result as? RunResult.ICPC)?.verdict?.isAccepted != true) } ?: emptyList() }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}