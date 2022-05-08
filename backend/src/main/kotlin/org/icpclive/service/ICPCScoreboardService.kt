package org.icpclive.service

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.merge
import org.icpclive.api.*
import org.icpclive.cds.OptimismLevel
import org.icpclive.config.loadMedalSettings
import org.icpclive.data.DataBus
import kotlin.math.max

abstract class ICPCScoreboardService(
    private val runsFlow: Flow<RunInfo>,
    optimismLevel: OptimismLevel
) {
    val flow = MutableStateFlow(Scoreboard(emptyList())).also {
        DataBus.setScoreboardEvents(optimismLevel, it)
    }
    val runs = mutableMapOf<Int, RunInfo>()

    abstract fun isAccepted(runInfo: RunInfo, index: Int, count: Int): Boolean
    abstract fun isPending(runInfo: RunInfo, index: Int, count: Int): Boolean
    abstract fun isAddingPenalty(runInfo: RunInfo, index: Int, count: Int): Boolean

    private val medalsSettings = loadMedalSettings()

    suspend fun run() {
        val contestInfoUpdates = DataBus.contestInfoUpdates.await()
        merge(runsFlow, contestInfoUpdates).collect { run ->
            if (run is RunInfo) {
                val oldRun = runs[run.id]
                runs[run.id] = run
                if (oldRun?.isJudged == false && !run.isJudged) {
                    return@collect
                }
            }
            flow.value = getScoreboard(contestInfoUpdates.value)
        }
    }

    private fun getScoreboardRow(problemsCount: Int, teamId: Int, runs: List<RunInfo>): ScoreboardRow {
        var solved = 0
        var penalty = 0
        var lastAccepted = 0L
        val runsByProblem = runs.groupBy { it.problemId }
        val problemResults = List(problemsCount) { problemId ->
            val problemRuns = runsByProblem.getOrElse(problemId) { emptyList() }
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
            ICPCProblemResult(
                runsBeforeFirstOk.withIndex().count { isAddingPenalty(it.value, it.index, problemRuns.size) },
                runsBeforeFirstOk.withIndex().count { isPending(it.value, it.index, problemRuns.size) },
                okRun != null,
                okRun?.isFirstSolvedRun == true,
                (okRun ?: runsBeforeFirstOk.lastOrNull())?.time
            ).also {
                if (it.isSolved) {
                    solved++
                    penalty += (okRun!!.time / 1000 / 60).toInt() + it.wrongAttempts * 20
                    lastAccepted = max(lastAccepted, okRun.time)
                }
            }
        }
        return ScoreboardRow(
            teamId,
            0,
            solved,
            penalty,
            lastAccepted,
            null,
            problemResults
        )

    }

    private fun getScoreboard(info: ContestInfo): Scoreboard {
        val runs = runs.values
            .sortedWith(compareBy({ it.time }, { it.id }))
            .groupBy { it.teamId }
        val teamsInfo = info.teams.associateBy { it.id }
        val comparator = compareBy<ScoreboardRow>(
            { -it.totalScore },
            { it.penalty },
            { it.lastAccepted }
        )

        val rows = teamsInfo.values
            .map { getScoreboardRow(info.problems.size, it.id, runs[it.id] ?: emptyList()) }
            .sortedWith(comparator.thenComparing { it: ScoreboardRow -> teamsInfo[it.teamId]!!.name })
            .toMutableList()
        if (rows.isNotEmpty()) {
            var rank = 1
            for (i in 0 until rows.size) {
                if (i != 0 && comparator.compare(rows[i - 1], rows[i]) < 0) {
                    rank++
                }
                val medal = medalsSettings.medalColorByRank(rank).takeIf { rows[i].totalScore > 0 }
                rows[i] = rows[i].copy(rank = rank, medalType = medal)
            }
        }
        return Scoreboard(rows)
    }
}

class ICPCNormalScoreboardService(runsFlow: Flow<RunInfo>) :
    ICPCScoreboardService(runsFlow, OptimismLevel.NORMAL) {
    override fun isAccepted(runInfo: RunInfo, index: Int, count: Int) = runInfo.isAccepted
    override fun isPending(runInfo: RunInfo, index: Int, count: Int) = !runInfo.isJudged
    override fun isAddingPenalty(runInfo: RunInfo, index: Int, count: Int) = runInfo.isJudged && runInfo.isAddingPenalty
}

class ICPCPessimisticScoreboardService(runsFlow: Flow<RunInfo>) :
    ICPCScoreboardService(runsFlow, OptimismLevel.PESSIMISTIC) {
    override fun isAccepted(runInfo: RunInfo, index: Int, count: Int) = runInfo.isAccepted
    override fun isPending(runInfo: RunInfo, index: Int, count: Int) = false
    override fun isAddingPenalty(runInfo: RunInfo, index: Int, count: Int) = !runInfo.isJudged || runInfo.isAddingPenalty
}

class ICPCOptimisticScoreboardService(runsFlow: Flow<RunInfo>) :
    ICPCScoreboardService(runsFlow, OptimismLevel.OPTIMISTIC) {
    override fun isAccepted(runInfo: RunInfo, index: Int, count: Int) =
        runInfo.isAccepted || (!runInfo.isJudged && index == count - 1)

    override fun isPending(runInfo: RunInfo, index: Int, count: Int) = false
    override fun isAddingPenalty(runInfo: RunInfo, index: Int, count: Int) =
        runInfo.isAddingPenalty || (!runInfo.isJudged && index != count - 1)
}
