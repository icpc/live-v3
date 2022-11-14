package org.icpclive.service

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.merge
import org.icpclive.api.*
import org.icpclive.data.DataBus
import kotlin.math.max
import kotlin.time.Duration

private fun List<MedalType>.medalColorByRank(rank_: Int): String? {
    var rank = rank_
    for ((color, count) in this) {
        if (rank <= count) return color
        rank -= count
    }
    return null
}

interface PenaltyCalculator {
    fun addSolvedProblem(time: Duration, wrongAttempts: Int)
    val penalty: Long
}

class EachSubmissionDownToMinutePenaltyCalculator(val penaltyPerWrongAttempt: Int) : PenaltyCalculator {
    override fun addSolvedProblem(time: Duration, wrongAttempts: Int) {
        penalty += time.inWholeMinutes + wrongAttempts * penaltyPerWrongAttempt
    }

    override var penalty = 0L
        private set

}

class SumDownToMinutePenaltyCalculator(val penaltyPerWrongAttempt: Int) : PenaltyCalculator {
    var penaltySeconds = 0L
    override fun addSolvedProblem(time: Duration, wrongAttempts: Int) {
        penaltySeconds += time.inWholeSeconds + wrongAttempts * penaltyPerWrongAttempt * 60L
    }

    override val penalty: Long
        get() = penaltySeconds / 60
}

abstract class ICPCScoreboardService(optimismLevel: OptimismLevel) {
    val flow = MutableStateFlow(Scoreboard(emptyList())).also {
        DataBus.setScoreboardEvents(optimismLevel, it)
    }
    val runs = mutableMapOf<Int, RunInfo>()

    abstract fun isAccepted(runInfo: RunInfo, index: Int, count: Int): Boolean
    abstract fun isPending(runInfo: RunInfo, index: Int, count: Int): Boolean
    abstract fun isAddingPenalty(runInfo: RunInfo, index: Int, count: Int): Boolean

    suspend fun run(
        runsFlow: Flow<RunInfo>,
        contestInfoFlow: Flow<ContestInfo>,
    ) {
        var info: ContestInfo? = null
        merge(runsFlow, contestInfoFlow).collect { update ->
            when (update) {
                is RunInfo -> {
                    val oldRun = runs[update.id]
                    runs[update.id] = update
                    if (oldRun?.isJudged == false && !update.isJudged) {
                        return@collect
                    }
                }
                is ContestInfo -> {
                    info = update
                }
            }
            info?.let {
                flow.value = getScoreboard(it)
            }
        }
    }

    private fun ContestInfo.getScoreboardRow(
        teamId: Int,
        runs: List<RunInfo>,
        teamGroups: List<String>,
        problems: List<ProblemInfo>
    ): ScoreboardRow {
        val penaltyCalculator = when (penaltyRoundingMode) {
            PenaltyRoundingMode.EACH_SUBMISSION_DOWN_TO_MINUTE -> EachSubmissionDownToMinutePenaltyCalculator(penaltyPerWrongAttempt)
            PenaltyRoundingMode.SUM_DOWN_TO_MINUTE -> SumDownToMinutePenaltyCalculator(penaltyPerWrongAttempt)
        }
        var solved = 0.0f
        var lastAccepted = 0L
        val runsByProblem = runs.groupBy { it.problemId }
        val problemResults = problems.map { problem ->
            val problemRuns = runsByProblem.getOrElse(problem.id) { emptyList() }
            val (runsBeforeFirstOk, okRun) = run {
                val okRunIndex = problemRuns
                    .withIndex()
                    .indexOfFirst { isAccepted(it.value, it.index, problemRuns.size) }
                if (okRunIndex == -1) {
                    problemRuns to null
                } else {
                    problemRuns.toList().subList(0, okRunIndex) to problemRuns[okRunIndex]
                }
            }

            val maxScore = if(problemRuns.isNotEmpty()) problemRuns.maxBy { it.score }.score else 0.0f

            return@map when(resultType) {
                ContestResultType.ICPC -> ICPCProblemResult(
                    runsBeforeFirstOk.withIndex().count { isAddingPenalty(it.value, it.index, problemRuns.size) },
                    runsBeforeFirstOk.withIndex().count { isPending(it.value, it.index, problemRuns.size) },
                    okRun != null,
                    okRun?.isFirstSolvedRun == true,
                    (okRun ?: runsBeforeFirstOk.lastOrNull())?.time
                ).also {
                    if (it.isSolved) {
                        solved++
                        penaltyCalculator.addSolvedProblem(okRun!!.time, it.wrongAttempts)
                        lastAccepted = max(lastAccepted, okRun.time.inWholeMilliseconds)
                    }
                }
                ContestResultType.IOI -> IOIProblemResult(
                    runsBeforeFirstOk.withIndex().count { isAddingPenalty(it.value, it.index, problemRuns.size) },
                    runsBeforeFirstOk.withIndex().count { isPending(it.value, it.index, problemRuns.size) },
                    maxScore,
                    okRun?.isFirstSolvedRun == true,
                    (okRun ?: runsBeforeFirstOk.lastOrNull())?.time
                ).also {
                    if (it.score > 0) {
                        solved += it.score
                        penaltyCalculator.addSolvedProblem(okRun!!.time, it.wrongAttempts)
                        lastAccepted = max(lastAccepted, okRun.time.inWholeMilliseconds)
                    }
                }
            }
        }
        return ScoreboardRow(
            teamId,
            0,
            solved,
            penaltyCalculator.penalty,
            lastAccepted,
            null,
            problemResults,
            teamGroups,
            emptyList()
        )
    }

    private fun getScoreboard(info: ContestInfo): Scoreboard {
        val runs = runs.values
            .sortedWith(compareBy({ it.time }, { it.id }))
            .groupBy { it.teamId }
        val teamsInfo = info.teams.filterNot { it.isHidden }.associateBy { it.id }
        val comparator = compareBy<ScoreboardRow>(
            { -it.totalScore },
            { it.penalty },
            { it.lastAccepted }
        )

        val allGroups = teamsInfo.values.flatMap { it.groups }.toMutableSet()

        val rows = teamsInfo.values
            .map { info.getScoreboardRow(it.id, runs[it.id] ?: emptyList(), it.groups, info.problems) }
            .sortedWith(comparator.thenComparing { it: ScoreboardRow -> teamsInfo[it.teamId]!!.name })
            .toMutableList()
        if (rows.isNotEmpty()) {
            var rank = 1
            for (i in 0 until rows.size) {
                if (i != 0 && comparator.compare(rows[i - 1], rows[i]) < 0) {
                    rank++
                }
                val medal = info.medals.medalColorByRank(rank)?.takeIf { rows[i].totalScore > 0 }
                val championInGroups = if (rows[i].totalScore > 0)
                    teamsInfo[rows[i].teamId]!!.groups.filter { it in allGroups }
                else
                    emptyList()
                for (group in championInGroups) {
                    allGroups -= group
                }
                rows[i] = rows[i].copy(rank = rank, medalType = medal, championInGroups = championInGroups)
            }
        }
        return Scoreboard(rows)
    }
}

class ICPCNormalScoreboardService : ICPCScoreboardService(OptimismLevel.NORMAL) {
    override fun isAccepted(runInfo: RunInfo, index: Int, count: Int) = runInfo.isAccepted
    override fun isPending(runInfo: RunInfo, index: Int, count: Int) = !runInfo.isJudged
    override fun isAddingPenalty(runInfo: RunInfo, index: Int, count: Int) = runInfo.isJudged && runInfo.isAddingPenalty
}

class ICPCPessimisticScoreboardService : ICPCScoreboardService(OptimismLevel.PESSIMISTIC) {
    override fun isAccepted(runInfo: RunInfo, index: Int, count: Int) = runInfo.isAccepted
    override fun isPending(runInfo: RunInfo, index: Int, count: Int) = false
    override fun isAddingPenalty(runInfo: RunInfo, index: Int, count: Int) = !runInfo.isJudged || runInfo.isAddingPenalty
}

class ICPCOptimisticScoreboardService : ICPCScoreboardService(OptimismLevel.OPTIMISTIC) {
    override fun isAccepted(runInfo: RunInfo, index: Int, count: Int) =
        runInfo.isAccepted || (!runInfo.isJudged && index == count - 1)

    override fun isPending(runInfo: RunInfo, index: Int, count: Int) = false
    override fun isAddingPenalty(runInfo: RunInfo, index: Int, count: Int) =
        runInfo.isAddingPenalty || (!runInfo.isJudged && index != count - 1)
}
