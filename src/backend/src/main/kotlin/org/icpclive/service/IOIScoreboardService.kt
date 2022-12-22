package org.icpclive.service

import org.icpclive.api.*

class IOIScoreboardService(optimismLevel: OptimismLevel) : ScoreboardService(optimismLevel) {
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
            val improvingRuns = problemRuns.filter { it.result != null && (it.result as IOIRunResult).difference > 0 }
            IOIProblemResult(
                improvingRuns.sumOf { (it.result as IOIRunResult).difference },
                improvingRuns.lastOrNull()?.time
            )
        }
        return ScoreboardRow(
            teamId,
            0,
            problemResults.sumOf { it.score },
            0,
            problemResults.maxOfOrNull { it.lastSubmitTime?.inWholeMilliseconds ?: 0 } ?: 0,
            null,
            problemResults,
            teamGroups,
            emptyList()
        )
    }
}

