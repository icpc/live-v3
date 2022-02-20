package org.icpclive.service

import kotlinx.coroutines.flow.*
import org.icpclive.DataBus
import org.icpclive.api.ICPCProblemResult
import org.icpclive.api.RunInfo
import org.icpclive.api.Scoreboard
import org.icpclive.api.ScoreboardRow
import org.icpclive.cds.OptimismLevel
import kotlin.math.max

abstract class ICPCScoreboardService(
    private val problemsCount: Int,
    private val runsFlow: Flow<RunInfo>,
    optimismLevel: OptimismLevel
    ) {
    val flow = MutableStateFlow(Scoreboard(emptyList()))
    init {
        DataBus.setScoreboardEvents(optimismLevel, flow)
    }
    val runs = mutableMapOf<Int, RunInfo>()

    abstract fun isAccepted(runInfo: RunInfo, index:Int, count:Int): Boolean
    abstract fun isPending(runInfo: RunInfo, index:Int, count:Int): Boolean
    abstract fun isAddingPenalty(runInfo: RunInfo, index:Int, count:Int): Boolean

    suspend fun run() {
        merge(runsFlow, DataBus.contestInfoFlow).collect { run ->
            if (run is RunInfo) {
                val oldRun = runs[run.id]
                runs[run.id] = run
                if (oldRun?.isJudged == false && !run.isJudged) {
                    return@collect
                }
            }
            flow.value = getScoreboard()
        }
    }

    private fun getScoreboardRow(teamId: Int, runs: List<RunInfo>) : ScoreboardRow {
        var solved = 0
        var penalty = 0
        var lastAccepted = 0L
        val runsByProblem = runs.groupBy { it.problemId }
        val problemResults = List(problemsCount) { problemId ->
            val problemRuns = runsByProblem.getOrElse(problemId) { emptyList() }
            val (runsBeforeFirstOk, okRun) = run {
                val okRunIndex = problemRuns.withIndex().indexOfFirst { isAccepted(it.value, it.index, problemRuns.size) }
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
            problemResults
        )

    }

    private fun getScoreboard() : Scoreboard {
        val runs = runs.values
            .sortedWith(compareBy({it.time}, {it.id}))
            .groupBy { it.teamId }
        val teamsInfo = DataBus.contestInfoFlow.value.teams.associateBy { it.id }
        val comparator = compareBy<ScoreboardRow>(
            { -it.totalScore },
            { it.penalty },
            { it.lastAccepted }
        )

        val rows = teamsInfo.values.
            map { getScoreboardRow(it.id, runs[it.id] ?: emptyList()) }.
            sortedWith(comparator.thenComparing { it: ScoreboardRow -> teamsInfo[it.teamId]!!.name })
            .toMutableList()
        if (rows.isNotEmpty()) {
            var rank = 1
            rows[0] = rows[0].copy(rank = 1)
            for (i in 1 until rows.size) {
                if (comparator.compare(rows[i-1], rows[i]) < 0) {
                    rank++
                }
                rows[i] = rows[i].copy(rank = rank)
            }
        }
        return Scoreboard(rows)
    }
}

class ICPCNormalScoreboardService(problemsCount: Int, runsFlow: Flow<RunInfo>):
        ICPCScoreboardService(problemsCount, runsFlow, OptimismLevel.NORMAL) {
    override fun isAccepted(runInfo: RunInfo, index: Int, count: Int) = runInfo.isAccepted
    override fun isPending(runInfo: RunInfo, index: Int, count: Int) = !runInfo.isJudged
    override fun isAddingPenalty(runInfo: RunInfo, index: Int, count: Int) = runInfo.isJudged && runInfo.isAddingPenalty
}

class ICPCPessimisticScoreboardService(problemsCount: Int, runsFlow: Flow<RunInfo>):
    ICPCScoreboardService(problemsCount, runsFlow, OptimismLevel.PESSIMISTIC) {
    override fun isAccepted(runInfo: RunInfo, index: Int, count: Int) = runInfo.isAccepted
    override fun isPending(runInfo: RunInfo, index: Int, count: Int) = false
    override fun isAddingPenalty(runInfo: RunInfo, index: Int, count: Int) = runInfo.isJudged && runInfo.isAddingPenalty
}

class ICPCOptimisticScoreboardService(problemsCount: Int, runsFlow: Flow<RunInfo>):
    ICPCScoreboardService(problemsCount, runsFlow, OptimismLevel.OPTIMISTIC) {
    override fun isAccepted(runInfo: RunInfo, index: Int, count: Int) = runInfo.isAccepted || (!runInfo.isJudged && index == count - 1)
    override fun isPending(runInfo: RunInfo, index: Int, count: Int) = false
    override fun isAddingPenalty(runInfo: RunInfo, index: Int, count: Int) = runInfo.isAddingPenalty || (!runInfo.isJudged && index != count - 1)
}