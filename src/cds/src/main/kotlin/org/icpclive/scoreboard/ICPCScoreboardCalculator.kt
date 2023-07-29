package org.icpclive.scoreboard

import org.icpclive.api.*
import kotlin.math.max


internal abstract class ICPCScoreboardCalculator : AbstractScoreboardCalculator() {
    abstract fun isAccepted(runInfo: RunInfo, index: Int, count: Int): Boolean
    abstract fun isPending(runInfo: RunInfo, index: Int, count: Int): Boolean
    abstract fun isAddingPenalty(runInfo: RunInfo, index: Int, count: Int): Boolean

    override val comparator: Comparator<ScoreboardRow> = compareBy(
        { -it.totalScore },
        { it.penalty },
        { it.lastAccepted }
    )

    override fun ContestInfo.getScoreboardRow(
        teamId: Int,
        runs: List<RunInfo>,
        teamGroups: List<String>,
        problems: List<ProblemInfo>
    ): ScoreboardRow {
        require(resultType == ContestResultType.ICPC)
        val penaltyCalculator = PenaltyCalculator.get(penaltyRoundingMode, penaltyPerWrongAttempt)
        var solved = 0
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

            ICPCProblemResult(
                runsBeforeFirstOk.withIndex().count { isAddingPenalty(it.value, it.index, problemRuns.size) },
                runsBeforeFirstOk.withIndex().count { isPending(it.value, it.index, problemRuns.size) },
                okRun != null,
                (okRun?.result as? ICPCRunResult)?.isFirstToSolveRun == true,
                (okRun ?: runsBeforeFirstOk.lastOrNull())?.time
            ).also {
                if (it.isSolved) {
                    solved++
                    penaltyCalculator.addSolvedProblem(okRun!!.time, it.wrongAttempts)
                    lastAccepted = max(lastAccepted, okRun.time.inWholeMilliseconds)
                }
            }
        }
        return ScoreboardRow(
            teamId,
            0,
            solved.toDouble(),
            penaltyCalculator.penalty,
            lastAccepted,
            null,
            problemResults,
            teamGroups,
            emptyList()
        )
    }
}

private val RunInfo.isAccepted get() = (result as? ICPCRunResult)?.verdict?.isAccepted == true
private val RunInfo.isAddingPenalty get() = (result as? ICPCRunResult)?.verdict?.isAddingPenalty == true
private val RunInfo.isJudged get() = result != null


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
