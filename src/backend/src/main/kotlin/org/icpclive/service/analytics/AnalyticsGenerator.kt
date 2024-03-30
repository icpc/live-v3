package org.icpclive.service.analytics

import kotlinx.coroutines.flow.*
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromStream
import org.icpclive.cds.api.*
import org.icpclive.cds.scoreboard.ContestStateWithScoreboard
import org.icpclive.cds.scoreboard.toScoreboardDiff
import org.icpclive.util.getLogger
import java.nio.file.Path
import kotlin.io.path.inputStream

class AnalyticsGenerator(jsonTemplatePath: Path) {
    private val messagesTemplates = Json.decodeFromStream<JsonAnalyticTemplates>(jsonTemplatePath.inputStream())

    suspend fun getFlow(
        contestInfoFlow: StateFlow<ContestInfo>,
        runsFlow: Flow<RunInfo>,
        scoreboardFlow: Flow<ContestStateWithScoreboard>,
    ) = flow {
        logger.info("Analytics generator service is started")
        val runs = mutableMapOf<RunId, RunAnalyse>()
        combine(contestInfoFlow, runsFlow, scoreboardFlow, ::Triple).collect { (contestInfo, run, scoreboard) ->
            if (run.isHidden) {
                runs.remove(run.id)
                return@collect
            }
            val analysis = runs.processRun(run, scoreboard.toScoreboardDiff(true)) ?: return@collect

            val team = contestInfo.teams[run.teamId] ?: return@collect
            val problem = contestInfo.problems[run.problemId] ?: return@collect
            emit(
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
            "{team.shortName}" to team.displayName,
            "{problem.letter}" to problem.displayName,
            "{problem.name}" to problem.fullName,
            "{run.result}" to (analyse.run.result as? RunResult.ICPC)?.verdict?.shortName.orEmpty(),
            "{result.rank}" to analyse.rank.toString(),
            "{result.solvedProblems}" to analyse.solvedProblems?.takeIf { it > 0 }?.toString().orEmpty(),
            "{result.ioiDifference}" to (analyse.run.result as? RunResult.IOI)?.difference?.toString().orEmpty(),
            "{result.ioiScore}" to (analyse.run.result as? RunResult.IOI)?.scoreAfter?.toString().orEmpty(),
        )
        return when (val runResult = analyse.run.result) {
            is RunResult.IOI -> {
                if (runResult.difference > 0) {
                    return messagesTemplates.ioiJudgedPositiveDiffRun.applyTemplate(substitute)
                } else {
                    return messagesTemplates.ioiJudgedRun.applyTemplate(substitute)
                }
            }
            is RunResult.ICPC -> {
                if (runResult.isFirstToSolveRun) {
                    messagesTemplates.firstToSolveRun.applyTemplate(substitute)
                } else if (runResult.verdict.isAccepted) {
                    return if (substitute["{result.solvedProblems}"] != "") {
                        messagesTemplates.acceptedWithSolvedProblemsRun.applyTemplate(substitute)
                    } else {
                        messagesTemplates.acceptedRun.applyTemplate(substitute)
                    }
                }
                return messagesTemplates.rejectedRun.applyTemplate(substitute)
            }
            is RunResult.InProgress -> messagesTemplates.submittedRun.applyTemplate(substitute)
        }
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
            }

            is RunResult.ICPC -> {
                if (!runResult.verdict.isAccepted) {
                    add("rejected")
                } else {
                    add("accepted")
                    if (runResult.isFirstToSolveRun) {
                        add("accepted-first-to-solve")
                    }

                    if (analyse.rank == 1) {
                        add("accepted-winner")
                    }
                    if (analyse.medalColor != null) {
                        add("accepted-medal")
                        add("accepted-${analyse.medalColor.name.lowercase()}-medal")
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
            val medal = scoreboard.awards.firstOrNull { it is Award.Medal && run.teamId in it.teams} as? Award.Medal
            RunAnalyse(
                run, Clock.System.now(), result,
                scoreboard.ranks[index],
                medal?.medalColor
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
        val medalColor: Award.Medal.MedalColor?,
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
