package org.icpclive.scoreboard

import org.icpclive.api.*

class IOIScoreboardCalculator : ScoreboardCalculator() {
    override val comparator: Comparator<ScoreboardRow> = compareBy(
        { -it.totalScore },
    )

    override fun ContestInfo.getScoreboardRow(
        teamId: Int,
        runs: List<RunInfo>,
        teamGroups: List<String>,
        problems: List<ProblemInfo>
    ): ScoreboardRow {
        require(resultType == ContestResultType.IOI)
        val runsByProblem = runs.groupBy { it.problemId }
        val problemResults = problems.map { problem ->
            val problemRuns = runsByProblem.getOrElse(problem.id) { emptyList() }
            val changingRuns = problemRuns.filter { it.result != null && (it.result as IOIRunResult).difference != 0.0 }
            IOIProblemResult(
                if (problemRuns.isEmpty()) null else changingRuns.sumOf { (it.result as IOIRunResult).difference },
                changingRuns.lastOrNull()?.time,
                changingRuns.find { (it.result as IOIRunResult).isFirstBestRun } != null
            )
        }
        return ScoreboardRow(
            teamId,
            0,
            problemResults.sumOf { it.score ?: 0.0 },
            0,
            problemResults.maxOfOrNull { it.lastSubmitTime?.inWholeMilliseconds ?: 0 } ?: 0,
            null,
            problemResults,
            teamGroups,
            emptyList()
        )
    }
}

