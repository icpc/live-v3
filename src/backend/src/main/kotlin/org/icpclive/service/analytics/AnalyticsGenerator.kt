package org.icpclive.service.analytics

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromStream
import org.icpclive.cds.RunUpdate
import org.icpclive.cds.api.*
import org.icpclive.cds.scoreboard.ContestStateWithScoreboard
import org.icpclive.cds.scoreboard.toScoreboardDiff
import java.nio.file.Path
import kotlin.io.path.inputStream
import kotlin.time.Clock
import kotlin.time.Instant

class AnalyticsGenerator(jsonTemplatePath: Path?) {
    private val messagesTemplates_ = jsonTemplatePath?.let { Json.decodeFromStream<JsonAnalyticTemplates>(it.inputStream()) }
    private val runs = mutableMapOf<RunId, RunAnalyse>()

    fun getMessages(state: ContestStateWithScoreboard): List<CommentaryMessage> {
        val event = state.state.lastEvent as? RunUpdate ?: return emptyList()
        if (messagesTemplates_ == null) return emptyList()
        val run = event.newInfo
        if (run.isHidden) return emptyList()
        val info = state.state.infoAfterEvent ?: return emptyList()
        val analysis = runs.processRun(run, state.toScoreboardDiff(true)) ?: return emptyList()

        val team = info.teams[run.teamId] ?: return emptyList()
        val problem = info.problems[run.problemId] ?: return emptyList()
        val message = CommentaryMessage(
            "_analytics_by_run_${run.id}".toCommentaryMessageId(),
            getMessage(messagesTemplates_, analysis, team, problem),
            analysis.creationTime,
            run.time,
            listOf(team.id),
            listOf(run.id),
            tags = getTags(analysis),
        )
        return listOf(message)
    }

    private fun getMessage(messagesTemplates: JsonAnalyticTemplates, analyse: RunAnalyse, team: TeamInfo, problem: ProblemInfo): String {
        val substitute = mapOf(
            "{team.shortName}" to team.displayName,
            "{problem.letter}" to problem.displayName,
            "{problem.name}" to problem.fullName,
            "{run.result}" to (analyse.run.result as? RunResult.ICPC)?.verdict?.shortName.orEmpty(),
            "{result.rank}" to analyse.rank.toString(),
            "{result.solvedProblems}" to analyse.solvedProblems?.takeIf { it > 0 }?.toString().orEmpty(),
            "{result.ioiDifference}" to (analyse.run.result as? RunResult.IOI)?.difference?.toString().orEmpty(),
            "{result.ioiScore}" to (analyse.run.result as? RunResult.IOI)?.scoreAfter?.toString().orEmpty(),
        )
        val template = when (val runResult = analyse.run.result) {
            is RunResult.IOI -> if (runResult.difference > 0) {
                messagesTemplates.ioiJudgedPositiveDiffRun
            } else {
                messagesTemplates.ioiJudgedRun
            }
            is RunResult.ICPC -> when {
                runResult.isFirstToSolveRun -> messagesTemplates.firstToSolveRun
                runResult.verdict.isAccepted -> if (substitute["{result.solvedProblems}"].isNullOrEmpty()) {
                    messagesTemplates.acceptedRun
                } else {
                    messagesTemplates.acceptedWithSolvedProblemsRun
                }
                else -> messagesTemplates.rejectedRun
            }
            is RunResult.InProgress -> messagesTemplates.submittedRun
        }
        return template.applyTemplate(substitute)
    }

    private fun getTags(
        analyse: RunAnalyse
    ): List<String> = buildList {
        when (val runResult = analyse.run.result) {
            is RunResult.IOI -> {
                add("submission")
                if (runResult.difference > 0) {
                    add("accepted")
                }
                if (runResult.isFirstBestRun) {
                    add("accepted-first-to-solve")
                }
            }

            is RunResult.InProgress -> {
                add("submission")
                for (i in analyse.awards) {
                    add("submission-${i.id}")
                }
            }

            is RunResult.ICPC -> {
                if (!runResult.verdict.isAccepted) {
                    add("rejected")
                } else {
                    add("accepted")
                    if (runResult.isFirstToSolveRun) {
                        add("accepted-first-to-solve")
                    }

                    for (i in analyse.awards) {
                        add("accepted-${i.id}")
                    }
                }
            }
        }
    }

    private fun MutableMap<RunId, RunAnalyse>.processRun(run: RunInfo, scoreboard: ScoreboardDiff): RunAnalyse? {
        val result = scoreboard.rows[run.teamId]
        if (result == null) {
            remove(run.id)
            return null
        }
        val index = scoreboard.order.indexOf(run.teamId)

        val analyse = getOrPut(run.id) {
            RunAnalyse(
                run, Clock.System.now(), result,
                scoreboard.ranks[index],
                scoreboard.awards.filter { run.teamId in it.teams }
            )
        }
        analyse.run = run
        analyse.solvedProblems = result.problemResults.count { it is ICPCProblemResult && it.isSolved }
        analyse.result = result
        return analyse
    }

    class RunAnalyse(
        var run: RunInfo,
        val creationTime: Instant,
        var result: ScoreboardRow,
        val rank: Int,
        val awards: List<Award>,
    ) {
        var solvedProblems: Int? = null
//        val rankDelta: Int
//            get() = result.let { after -> resultBefore.let { before -> after.rank - before.rank } }
    }

    interface AnalyticsTemplates {
        val submittedRun: String
        val rejectedRun: String
        val firstToSolveRun: String
        val acceptedRun: String
        val acceptedWithSolvedProblemsRun: String
        val ioiJudgedRun: String
        val ioiJudgedPositiveDiffRun: String
    }

    @Serializable
    class JsonAnalyticTemplates(
        override val submittedRun: String,
        override val rejectedRun: String,
        override val firstToSolveRun: String,
        override val acceptedRun: String,
        override val acceptedWithSolvedProblemsRun: String,
        override val ioiJudgedRun: String,
        override val ioiJudgedPositiveDiffRun: String,
    ) : AnalyticsTemplates

    companion object {
        fun String.applyTemplate(substitute: Map<String, String>) = substitute.entries.fold(this) { text, it ->
            text.replace(it.key, it.value)
        }

    }
}
