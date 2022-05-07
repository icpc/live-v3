package org.icpclive.service

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.*
import org.icpclive.api.*
import org.icpclive.data.DataBus
import org.icpclive.utils.completeOrThrow

class StatisticsService(
    private val scoreboardFlow: Flow<Scoreboard>,
    private val contestInfoFlow: Flow<ContestInfo>
) {
    suspend fun launch(scope: CoroutineScope) {
        scoreboardFlow
            .combine(contestInfoFlow.map { it.problems.size }.distinctUntilChanged()) { scoreboard, problemNumber ->
                SolutionsStatistic(
                    List(problemNumber) { problem ->
                        val results =
                            scoreboard.rows.asSequence().map { it.problemResults[problem] as ICPCProblemResult }
                        ProblemSolutionsStatistic(
                            results.count { it.isSolved },
                            results.count { !it.isSolved && it.wrongAttempts > 0 && it.pendingAttempts == 0 },
                            results.count { !it.isSolved && it.pendingAttempts > 0 },
                        )
                    }
                )
            }.stateIn(scope)
            .also { DataBus.statisticFlow.completeOrThrow(it) }
    }
}