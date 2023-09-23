package org.icpclive.org.icpclive.export.pcms

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.*
import org.icpclive.api.*
import org.icpclive.cds.ContestUpdate
import org.icpclive.cds.adapters.*
import org.icpclive.scoreboard.getScoreboardCalculator
import org.icpclive.util.createChild
import org.w3c.dom.Element
import java.io.StringWriter
import javax.xml.parsers.DocumentBuilderFactory
import javax.xml.transform.OutputKeys
import javax.xml.transform.TransformerFactory
import javax.xml.transform.dom.DOMSource
import javax.xml.transform.stream.StreamResult
import kotlin.time.Duration


object PCMSExporter {

    private fun convertOutcome(outcome: Verdict?) = when (outcome) {
        null -> "unknown"
        Verdict.Accepted -> "accepted"
        Verdict.Fail -> "fail"
        Verdict.CompilationError -> "compilation-error"
        Verdict.WrongAnswer -> "wrong-answer"
        Verdict.PresentationError -> "presentation-error"
        Verdict.RuntimeError -> "runtime-error"
        Verdict.TimeLimitExceeded -> "time-limit-exceeded"
        Verdict.MemoryLimitExceeded -> "memory-limit-exceeded"
        Verdict.OutputLimitExceeded -> "output-limit-exceeded"
        Verdict.IdlenessLimitExceeded -> "idleness-limit-exceeded"
        Verdict.SecurityViolation -> "security-violation"
        Verdict.Challenged -> "wrong-answer"
        Verdict.CompilationErrorWithPenalty -> "wrong-answer"
        Verdict.Ignored -> "compilation-error"
        Verdict.Rejected -> "wrong-answer"
    }

    fun ContestStatus.toPcmsStatus() = when (this) {
        ContestStatus.FINALIZED, ContestStatus.OVER -> "over"
        ContestStatus.RUNNING -> "running"
        ContestStatus.BEFORE -> "before"
    }


    private fun Element.buildContestNode(info: ContestInfo) {
        setAttribute("name", info.name)
        setAttribute("time", info.currentContestTime.inWholeMilliseconds.toString())
        setAttribute("start-time", info.startTime.toString())
        setAttribute("start-time-millis", info.startTime.toEpochMilliseconds().toString())
        setAttribute("length", info.contestLength.inWholeSeconds.toString())
        setAttribute("status", info.status.toPcmsStatus())
        setAttribute("frozen", "no")
        setAttribute("freeze-time", info.freezeTime.toIsoString())
        setAttribute("freeze-time-millis", info.freezeTime.inWholeMilliseconds.toString())
    }
    private fun Element.buildChallengeNode(info: ContestInfo) {
        info.scoreboardProblems.forEach { problem ->
            createChild("problem").also {
                it.setAttribute("alias", problem.displayName)
                it.setAttribute("name", problem.fullName)
            }
        }
    }
    private fun Element.buildRunNode(info: RunInfo) {
        setAttribute("accepted", if ((info.result as? ICPCRunResult)?.verdict?.isAccepted == true) "yes" else "no")
        setAttribute("time", info.time.inWholeMilliseconds.toString())
        setAttribute("score", "0")
        //setAttribute("language-id", "")
        //setAttribute("run-id", "")
        setAttribute("outcome", convertOutcome((info.result as? ICPCRunResult)?.verdict))
    }

    private fun Element.buildSessionNode(info: ContestInfo, teamInfo: TeamInfo, row: ScoreboardRow, runs: List<RunInfo>) {
        setAttribute("party", teamInfo.fullName)
        setAttribute("id", teamInfo.contestSystemId)
        // setAttribute("time", "")
        setAttribute("alias", teamInfo.contestSystemId)
        setAttribute("penalty", row.penalty.inWholeMinutes.toString())
        setAttribute("solved", row.totalScore.toInt().toString())
        val runsByProblem = runs.groupBy { it.problemId }
        row.problemResults.forEachIndexed { index, probResult ->
            val probNode = createChild("problem")
            val problemRuns = (runsByProblem[index] ?: emptyList())
            val isAcceptedInt = if ((probResult as ICPCProblemResult).isSolved) 1 else 0
            probNode.setAttribute("accepted", isAcceptedInt.toString())
            probNode.setAttribute("attempts", (probResult.wrongAttempts + isAcceptedInt).toString())
            probNode.setAttribute("id", info.scoreboardProblems[index].contestSystemId)
            probNode.setAttribute("alias", info.scoreboardProblems[index].displayName)
            probNode.setAttribute("time", (probResult.lastSubmitTime ?: Duration.ZERO).inWholeMilliseconds.toString())
            probNode.setAttribute("penalty", (if (probResult.isSolved) {
                (probResult.lastSubmitTime!! + info.penaltyPerWrongAttempt * probResult.wrongAttempts).inWholeMinutes
            } else 0).toString())
            problemRuns.forEach {
                probNode.createChild("run").apply {
                    buildRunNode(it)
                }
            }
        }
    }


    fun format(info: ContestInfo, runsByTeam: Map<Int, List<RunInfo>>) : String {
        if (info.resultType == ContestResultType.IOI) TODO("IOI is not supported yet")
        val documentFactory = DocumentBuilderFactory.newInstance()!!
        val documentBuilder = documentFactory.newDocumentBuilder()!!
        val document = documentBuilder.newDocument()!!
        val root = document.createElement("standings")
        document.appendChild(root)

        val contest = root.createChild("contest")
        contest.buildContestNode(info)
        val challenge = contest.createChild("challenge")
        challenge.buildChallengeNode(info)
        val scoreboardCalculator = getScoreboardCalculator(info, OptimismLevel.NORMAL)
        val rows = info.teams.keys.associateWith { scoreboardCalculator.getScoreboardRow(info, runsByTeam[it] ?: emptyList()) }
        val ranking = scoreboardCalculator.getRanking(info, rows)
        ranking.order.forEach {
            contest.createChild("session").also { session ->
                session.buildSessionNode(info, info.teams[it]!!, rows[it]!!, runsByTeam[it] ?: emptyList())
            }
        }

        val transformerFactory = TransformerFactory.newInstance()!!
        val transformer = transformerFactory.newTransformer()!!
        transformer.setOutputProperty(OutputKeys.INDENT, "yes")
        transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8")
        val domSource = DOMSource(document)
        val output = StringWriter()
        val streamResult = StreamResult(output)

        transformer.transform(domSource, streamResult)
        return output.toString()
    }

    fun Route.setUp(scope: CoroutineScope, contestUpdates: Flow<ContestUpdate>) {
        val stateFlow = contestUpdates
            .stateGroupedByTeam()
            .stateIn(scope, SharingStarted.Eagerly, null)
            .filterNotNull()
            .filter { it.infoAfterEvent != null }
        get {
            call.respondRedirect("/pcms/standings.xml", permanent = true)
        }
        get("standings.xml") {
            call.respondText(contentType = ContentType.Text.Xml) {
                val state = stateFlow.first()
                format(
                    state.infoAfterEvent!!,
                    state.runs
                )
            }
        }
    }
}