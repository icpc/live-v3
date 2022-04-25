package org.icpclive.service

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import org.icpclive.api.ICPCProblemResult
import org.icpclive.api.ProblemSolutionsStatistic
import org.icpclive.api.Scoreboard
import org.icpclive.api.SolutionsStatistic
import org.icpclive.data.DataBus
import org.icpclive.utils.completeOrThrow

class StatisticsService(private val problemNumber: Int, private val scoreboardFlow: Flow<Scoreboard>) {
    val result = MutableStateFlow(SolutionsStatistic(List(problemNumber) {
        ProblemSolutionsStatistic(0, 0, 0)
    })).also { DataBus.statisticFlow.completeOrThrow(it) }

    suspend fun run() {
        scoreboardFlow.collect { scoreboard ->
            result.value = SolutionsStatistic(
                List(problemNumber) { problem ->
                    val results = scoreboard.rows.asSequence().map { it.problemResults[problem] as ICPCProblemResult }
                    ProblemSolutionsStatistic(
                        results.count { it.isSolved },
                        results.count { !it.isSolved && it.wrongAttempts > 0 && it.pendingAttempts == 0 },
                        results.count { !it.isSolved && it.pendingAttempts > 0 },
                    )
                }
            )
        }
    }
}