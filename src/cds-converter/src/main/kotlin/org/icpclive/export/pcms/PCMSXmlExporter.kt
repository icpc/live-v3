package org.icpclive.export.pcms

import io.ktor.http.*
import org.icpclive.cds.api.*
import org.icpclive.cds.scoreboard.ContestStateWithScoreboard
import org.icpclive.cds.util.createChild
import org.icpclive.export.SingleFileExporter
import org.w3c.dom.Element
import java.io.StringWriter
import javax.xml.parsers.DocumentBuilderFactory
import javax.xml.transform.OutputKeys
import javax.xml.transform.TransformerFactory
import javax.xml.transform.dom.DOMSource
import javax.xml.transform.stream.StreamResult
import kotlin.time.Duration


object PCMSXmlExporter : SingleFileExporter("standings.xml", ContentType.Text.Xml) {

    private fun convertOutcome(outcome: Verdict?) = when (outcome) {
        null -> "undefined"
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
        Verdict.Challenged -> "unknown"
        Verdict.CompilationErrorWithPenalty -> "unknown"
        Verdict.Ignored -> "compilation-error"
        Verdict.Rejected -> "unknown"
    }

    fun ContestStatus.toPcmsStatus() = when (this) {
        is ContestStatus.FINALIZED, is ContestStatus.OVER -> "over"
        is ContestStatus.RUNNING -> "running"
        is ContestStatus.BEFORE -> "before"
    }

    private fun Element.setAttributeIfNotNull(name: String, value: String?) {
        if (value != null) {
            setAttribute(name, value)
        }
    }

    private fun Element.buildContestNode(info: ContestInfo) {
        setAttribute("name", info.name)
        setAttribute("time", info.currentContestTime.inWholeMilliseconds.toString())
        setAttributeIfNotNull("start-time", info.startTime?.toString())
        setAttributeIfNotNull("start-time-millis", info.startTime?.toEpochMilliseconds().toString())
        setAttribute("length", info.contestLength.inWholeMilliseconds.toString())
        setAttribute("status", info.status.toPcmsStatus())
        setAttribute("frozen", "no")
        setAttributeIfNotNull("freeze-time", info.freezeTime?.toIsoString())
        setAttributeIfNotNull("freeze-time-millis", info.freezeTime?.inWholeMilliseconds.toString())
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
        setAttribute("accepted", when (val result = info.result) {
            is RunResult.ICPC -> if (result.verdict.isAccepted) "yes" else "no"
            is RunResult.IOI -> if (result.wrongVerdict == null) "yes" else "no"
            is RunResult.InProgress -> "undefined"
        })
        setAttribute("time", info.time.inWholeMilliseconds.toString())
        setAttribute("score", when (val result = info.result) {
            is RunResult.ICPC, is RunResult.InProgress -> "0"
            is RunResult.IOI -> result.score.sum().toString()
        })
        setAttributeIfNotNull("language-id", info.languageId?.value)
        setAttribute("run-id", info.id.toString())
        setAttribute("outcome", convertOutcome((info.result as? RunResult.ICPC)?.verdict))
    }

    private fun Element.buildSessionNode(info: ContestInfo, teamInfo: TeamInfo, row: ScoreboardRow, runs: List<RunInfo>, awards: List<Award>) {
        setAttribute("party", teamInfo.fullName)
        setAttribute("id", teamInfo.id.value)
        // setAttribute("time", "")
        setAttribute("alias", teamInfo.id.value)
        setAttribute("penalty", row.penalty.inWholeMinutes.toString())
        setAttribute("solved", row.totalScore.toInt().toString())
        for (award in awards) {
            KnownAwards.entries.singleOrNull { it.awardId == award.id }?.let {
                setAttribute(it.xmlAttribute, "1")
            }
        }
        val runsByProblem = runs.groupBy { it.problemId }
        row.problemResults.forEachIndexed { index, probResult ->
            val probNode = createChild("problem")
            val problemInfo = info.scoreboardProblems[index]
            val problemRuns = (runsByProblem[problemInfo.id] ?: emptyList())
            val isAcceptedInt = if ((probResult as ICPCProblemResult).isSolved) 1 else 0
            probNode.setAttribute("accepted", isAcceptedInt.toString())
            probNode.setAttribute("attempts", (probResult.wrongAttempts + isAcceptedInt).toString())
            probNode.setAttribute("id", problemInfo.id.value)
            probNode.setAttribute("alias", problemInfo.displayName)
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


    override fun format(state: ContestStateWithScoreboard) : String {
        val info = state.state.infoAfterEvent!!
        val ranking = state.rankingAfter
        val runsByTeam = state.state.runsAfterEvent.values.groupBy { it.teamId }
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
        ranking.order.forEach {
            contest.createChild("session").also { session ->
                session.buildSessionNode(
                    info = info,
                    teamInfo = info.teams[it]!!,
                    row = state.scoreboardRowAfter(it),
                    runs = runsByTeam[it] ?: emptyList(),
                    awards = ranking.awards.filter { award -> it in award.teams }
                )
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

}