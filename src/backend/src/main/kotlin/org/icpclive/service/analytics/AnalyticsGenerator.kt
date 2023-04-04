package org.icpclive.service.analytics

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromStream
import org.icpclive.api.*
import org.icpclive.util.getLogger
import java.nio.file.Path
import kotlin.io.path.inputStream

class AnalyticsGenerator(jsonTemplatePath: Path) {
    private val messagesTemplates = Json.decodeFromStream<JsonAnalyticTemplates>(jsonTemplatePath.inputStream())

    suspend fun run(
        analyticsMessagesFlow: FlowCollector<AnalyticsMessage>,
        contestInfoFlow: StateFlow<ContestInfo>,
        runsFlow: Flow<RunInfo>,
        scoreboardFlow: Flow<Scoreboard>,
    ) {
        logger.info("Analytics generator service is started")
        val runs = mutableMapOf<Int, RunAnalyse>()
        combine(contestInfoFlow, runsFlow, scoreboardFlow, ::Triple).collect { (contestInfo, run, scoreboard) ->
            if (run.isHidden) {
                runs.remove(run.id)
                return@collect
            }
            val analysis = runs.processRun(run, scoreboard) ?: return@collect

            val team = contestInfo.teams.firstOrNull { it.id == run.teamId } ?: return@collect
            val problem = contestInfo.problems.firstOrNull { it.id == run.problemId } ?: return@collect
            analyticsMessagesFlow.emit(
                AnalyticsCommentaryEvent(
                    "_analytics_by_run_${run.id}",
                    getMessage(analysis, team, problem),
                    analysis.creationTime,
                    run.time,
                    listOf(team.id),
                    listOf(run.id),
                    tags = getTags(analysis),
                )
            )
        }
    }

    private fun getMessage(analyse: RunAnalyse, team: TeamInfo, problem: ProblemInfo): String {
        val substitute = mapOf(
            "{team.shortName}" to team.shortName,
            "{problem.letter}" to problem.letter,
            "{problem.name}" to problem.name,
            "{run.result}" to (analyse.run.result as? ICPCRunResult)?.result.orEmpty(),
            "{result.rank}" to analyse.result.rank.toString(),
            "{result.solvedProblems}" to analyse.solvedProblems?.takeIf { it > 0 }?.toString().orEmpty(),
            "{result.ioiDifference}" to (analyse.run.result as? IOIRunResult)?.difference?.toString().orEmpty(),
            // TODO: not just sum scores by group. We should use accumulator
            "{result.ioiScore}" to (analyse.run.result as? IOIRunResult)?.score?.sum()?.toString().orEmpty(),
        )
        val runResult = analyse.run.result
        if (runResult is IOIRunResult && runResult.difference > 0) {
            return messagesTemplates.ioiJudgedPositiveDiffRun.applyTemplate(substitute)
        }
        if (runResult !is ICPCRunResult) {
            return messagesTemplates.ioiJudgedRun.applyTemplate(substitute)
        }
        if (runResult.isFirstToSolveRun) {
            return messagesTemplates.firstToSolveRun.applyTemplate(substitute)
        }
        if (runResult.isAccepted) {
            if (substitute["{result.solvedProblems}"] != "") {
                return messagesTemplates.acceptedWithSolvedProblemsRun.applyTemplate(substitute)
            }
            return messagesTemplates.acceptedRun.applyTemplate(substitute)
        }
        return messagesTemplates.rejectedRun.applyTemplate(substitute)
    }

    private fun getTags(
        analyse: RunAnalyse
    ): List<String> = buildList {
        val runResult = analyse.run.result
        if (runResult is IOIRunResult) {
            add("submission")
            if (runResult.difference > 0) {
                add("accepted")
            }
            if (runResult.isFirstBestRun) {
                add("accepted-first-to-solve")
            }
            return@buildList
        }
        if (runResult !is ICPCRunResult) {
            add("submission")
            return@buildList
        }
        if (!runResult.isAccepted) {
            add("rejected")
            return@buildList
        }
        add("accepted")
        if (runResult.isFirstToSolveRun) {
            add("accepted-first-to-solve")
        }
        if (analyse.result.rank == 1) {
            add("accepted-winner")
        }
        if (analyse.result.medalType != null) {
            add("accepted-medal")
            add("accepted-${analyse.result.medalType}-medal")
        }
    }

    private fun MutableMap<Int, RunAnalyse>.processRun(run: RunInfo, scoreboard: Scoreboard): RunAnalyse? {
        val result = scoreboard.rows.firstOrNull { it.teamId == run.teamId }
        if (result == null) {
            remove(run.id)
            return null
        }

        val analyse = getOrPut(run.id) { RunAnalyse(run, Clock.System.now(), result, result) }
        analyse.run = run
        analyse.solvedProblems = result.problemResults.count { it is ICPCProblemResult && it.isSolved }
        analyse.result = result
        return analyse
    }

    class RunAnalyse(
        var run: RunInfo,
        val creationTime: Instant,
        var resultBefore: ScoreboardRow,
        var result: ScoreboardRow
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

        val logger = getLogger(AnalyticsGenerator::class)
    }
}
