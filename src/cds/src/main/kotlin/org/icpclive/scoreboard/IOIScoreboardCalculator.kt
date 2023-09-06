package org.icpclive.scoreboard

import org.icpclive.api.*
import kotlin.time.Duration

internal class IOIScoreboardCalculator : AbstractScoreboardCalculator() {
    override val comparator: Comparator<ScoreboardRow> = compareBy(
        { -it.totalScore },
        { it.penalty }
    )

    override fun getScoreboardRow(
        info: ContestInfo,
        runs: List<RunInfo>
    ): ScoreboardRow {
        require(info.resultType == ContestResultType.IOI)
        val penaltyCalculator = PenaltyCalculator.get(
            info.penaltyRoundingMode,
            info.penaltyPerWrongAttempt
        )
        val runsByProblem = runs.groupBy { it.problemId }
        val problemResults = info.scoreboardProblems.map { problem ->
            val problemRuns = runsByProblem.getOrElse(problem.id) { emptyList() }
            val finalRunIndex = problemRuns.indexOfLast { it.result != null && (it.result as IOIRunResult).difference != 0.0 }
            val finalRun = if (finalRunIndex == -1) null else problemRuns[finalRunIndex]
            if (finalRun != null) {
                penaltyCalculator.addSolvedProblem(finalRun.time, problemRuns.subList(0, finalRunIndex).count { (it.result as? IOIRunResult)?.wrongVerdict?.isAddingPenalty == true })
            }
            IOIProblemResult(
                (finalRun?.result as? IOIRunResult?)?.scoreAfter,
                finalRun?.time,
                (finalRun?.result as? IOIRunResult?)?.isFirstBestRun == true
            )
        }
        return ScoreboardRow(
            problemResults.sumOf { it.score ?: 0.0 },
            penaltyCalculator.penalty,
            problemResults.maxOfOrNull { it.lastSubmitTime ?: Duration.ZERO } ?: Duration.ZERO,
            problemResults,
        )
    }
}

