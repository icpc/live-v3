package org.icpclive.service

import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.*
import org.icpclive.api.*
import org.icpclive.data.DataBus
import org.icpclive.utils.completeOrThrow

class StatisticsService {
    suspend fun run(scoreboardFlow: Flow<Scoreboard>, contestInfoFlow: Flow<ContestInfo>) {
        coroutineScope {
            combine(
                scoreboardFlow,
                contestInfoFlow.map { it.problems.size }.distinctUntilChanged(),
                ::Pair,
            ).map { (scoreboard, problemNumber) ->
                if (scoreboard.rows.isEmpty()) {
                    SolutionsStatistic(List(problemNumber) { ProblemSolutionsStatistic(0, 0, 0) })
                } else {
                    SolutionsStatistic(
                        List(scoreboard.rows[0].problemResults.size) { problem ->
                            val results =
                                scoreboard.rows.asSequence().map { it.problemResults[problem] as ICPCProblemResult }
                            ProblemSolutionsStatistic(
                                results.count { it.isSolved },
                                results.count { !it.isSolved && it.wrongAttempts > 0 && it.pendingAttempts == 0 },
                                results.count { !it.isSolved && it.pendingAttempts > 0 },
                            )
                        }
                    )
                }
            }.stateIn(this)
                .also { DataBus.statisticFlow.completeOrThrow(it) }
        }
    }
}