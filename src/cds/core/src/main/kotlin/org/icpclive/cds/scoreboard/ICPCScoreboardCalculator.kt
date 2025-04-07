package org.icpclive.cds.scoreboard

import org.icpclive.cds.api.*
import kotlin.time.Duration


internal abstract class ICPCScoreboardCalculator : AbstractScoreboardCalculator() {
    abstract fun isAccepted(runInfo: RunInfo, index: Int, count: Int): Boolean
    abstract fun isPending(runInfo: RunInfo, index: Int, count: Int): Boolean
    abstract fun isAddingPenalty(runInfo: RunInfo, index: Int, count: Int): Boolean

    override val comparator: Comparator<ScoreboardRow> = compareBy(
        { -it.totalScore },
        { it.penalty },
        { it.lastAccepted }
    )

    override fun getScoreboardRow(
        info: ContestInfo,
        runs: List<RunInfo>,
    ): ScoreboardRow {
        require(info.resultType == ContestResultType.ICPC)
        val penaltyCalculator = PenaltyCalculator.get(
            info.penaltyRoundingMode,
            info.penaltyPerWrongAttempt
        )
        var solved = 0
        var lastAccepted = Duration.ZERO
        val runsByProblem = runs.filterNot { it.isHidden }.groupBy { it.problemId }
        val problemResults = info.scoreboardProblems.map { problem ->
            val problemRuns = runsByProblem.getOrElse(problem.id) { emptyList() }
            val (runsBeforeFirstOk, okRun) = info.run {
                val okRunIndex = problemRuns
                    .withIndex()
                    .indexOfFirst { this@ICPCScoreboardCalculator.isAccepted(it.value, it.index, problemRuns.size) }
                if (okRunIndex == -1) {
                    problemRuns to null
                } else {
                    problemRuns.toList().subList(0, okRunIndex) to problemRuns[okRunIndex]
                }
            }

            ICPCProblemResult(
                wrongAttempts = runsBeforeFirstOk.withIndex().count { isAddingPenalty(it.value, it.index, problemRuns.size) },
                pendingAttempts = runsBeforeFirstOk.withIndex().count { isPending(it.value, it.index, problemRuns.size) },
                isSolved = okRun != null,
                isFirstToSolve = (okRun?.result as? RunResult.ICPC)?.isFirstToSolveRun == true,
                lastSubmitTime = (okRun ?: runsBeforeFirstOk.lastOrNull())?.time
            ).also {
                if (it.isSolved) {
                    solved += problem.weight
                    penaltyCalculator.addSolvedProblem(okRun!!.time, it.wrongAttempts)
                    lastAccepted = maxOf(lastAccepted, okRun.time)
                }
            }
        }
        return ScoreboardRow(
            totalScore = solved.toDouble(),
            penalty = penaltyCalculator.penalty,
            lastAccepted = lastAccepted,
            problemResults = problemResults,
        )
    }
}

private val RunInfo.isAccepted get() = (result as? RunResult.ICPC)?.verdict?.isAccepted == true
private val RunInfo.isAddingPenalty get() = (result as? RunResult.ICPC)?.verdict?.isAddingPenalty == true
private val RunInfo.isJudged get() = result is RunResult.ICPC


internal class ICPCNormalScoreboardCalculator : ICPCScoreboardCalculator() {
    override fun isAccepted(runInfo: RunInfo, index: Int, count: Int) = runInfo.isAccepted
    override fun isPending(runInfo: RunInfo, index: Int, count: Int) = !runInfo.isJudged
    override fun isAddingPenalty(runInfo: RunInfo, index: Int, count: Int) = runInfo.isJudged && runInfo.isAddingPenalty
}

internal class ICPCPessimisticScoreboardCalculator : ICPCScoreboardCalculator() {
    override fun isAccepted(runInfo: RunInfo, index: Int, count: Int) = runInfo.isAccepted
    override fun isPending(runInfo: RunInfo, index: Int, count: Int) = false
    override fun isAddingPenalty(runInfo: RunInfo, index: Int, count: Int) = !runInfo.isJudged || runInfo.isAddingPenalty
}

internal class ICPCOptimisticScoreboardCalculator : ICPCScoreboardCalculator() {
    override fun isAccepted(runInfo: RunInfo, index: Int, count: Int) =
        runInfo.isAccepted || (!runInfo.isJudged && index == count - 1)

    override fun isPending(runInfo: RunInfo, index: Int, count: Int) = false
    override fun isAddingPenalty(runInfo: RunInfo, index: Int, count: Int) =
        runInfo.isAddingPenalty || (!runInfo.isJudged && index != count - 1)
}
