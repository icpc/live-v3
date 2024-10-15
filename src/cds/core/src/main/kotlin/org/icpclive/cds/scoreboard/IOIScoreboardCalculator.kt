package org.icpclive.cds.scoreboard

import org.icpclive.cds.api.*
import kotlin.time.Duration

internal class IOIScoreboardCalculator : AbstractScoreboardCalculator() {
    override val comparator: Comparator<ScoreboardRow> = compareBy(
        { -it.totalScore },
        { it.penalty }
    )

    override fun getScoreboardRow(
        info: ContestInfo,
        runs: List<RunInfo>,
    ): ScoreboardRow {
        require(info.resultType == ContestResultType.IOI)
        val penaltyCalculator = PenaltyCalculator.get(
            info.penaltyRoundingMode,
            info.penaltyPerWrongAttempt
        )
        val runsByProblem = runs.filterNot { it.isHidden }.groupBy { it.problemId }
        val problemResults = info.scoreboardProblems.map { problem ->
            val problemRuns = runsByProblem.getOrElse(problem.id) { emptyList() }
            val finalRunIndex = problemRuns.indexOfLast { it.result is RunResult.IOI && it.result.difference != 0.0 }
            val finalRun = if (finalRunIndex == -1) null else problemRuns[finalRunIndex]
            if (finalRun != null) {
                penaltyCalculator.addSolvedProblem(
                    finalRun.time,
                    problemRuns.subList(0, finalRunIndex).count { (it.result as? RunResult.IOI)?.wrongVerdict?.isAddingPenalty == true })
            }
            IOIProblemResult(
                (finalRun?.result as? RunResult.IOI?)?.scoreAfter,
                finalRun?.time,
                (finalRun?.result as? RunResult.IOI?)?.isFirstBestRun == true
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

