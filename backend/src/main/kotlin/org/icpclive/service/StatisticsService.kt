package org.icpclive.service

import kotlinx.coroutines.flow.*
import org.icpclive.api.*
import org.icpclive.data.DataBus

class StatisticsService(private val problemNumber: Int, private val scoreboardFlow: Flow<Scoreboard>) {
    val result = MutableStateFlow(SolutionsStatistic(List(problemNumber) {
        ProblemSolutionsStatistic(0, 0, 0)
    })).also { DataBus.statisticFlow.set(it) }

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