package org.icpclive.scoreboard

import org.icpclive.api.*

internal class IOIScoreboardCalculator : AbstractScoreboardCalculator() {
    override val comparator: Comparator<ScoreboardRow> = compareBy(
        { -it.totalScore },
        { it.penalty }
    )

    override fun ContestInfo.getScoreboardRow(
        teamId: Int,
        runs: List<RunInfo>,
        teamGroups: List<String>,
        problems: List<ProblemInfo>
    ): ScoreboardRow {
        require(resultType == ContestResultType.IOI)
        val penaltyCalculator = PenaltyCalculator.get(penaltyRoundingMode, penaltyPerWrongAttempt)
        val runsByProblem = runs.groupBy { it.problemId }
        val problemResults = problems.map { problem ->
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
            teamId,
            0,
            problemResults.sumOf { it.score ?: 0.0 },
            penaltyCalculator.penalty,
            problemResults.maxOfOrNull { it.lastSubmitTime?.inWholeMilliseconds ?: 0 } ?: 0,
            null,
            problemResults,
            teamGroups,
            emptyList()
        )
    }
}

