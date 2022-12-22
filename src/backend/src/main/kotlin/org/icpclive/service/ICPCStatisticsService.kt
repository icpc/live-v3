package org.icpclive.service

import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.*
import org.icpclive.api.*
import org.icpclive.util.completeOrThrow
import org.icpclive.data.DataBus

class ICPCStatisticsService {
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
                        List(scoreboard.rows[0].problemResults.size) { problemId ->
                            var success = 0
                            var wrong = 0
                            var pending = 0

                            for(row in scoreboard.rows) {
                                val p = row.problemResults[problemId]
                                require(p is ICPCProblemResult)
                                success += if (p.isSolved) 1 else 0
                                wrong += if (!p.isSolved && p.wrongAttempts > 0 && p.pendingAttempts == 0) 1 else 0
                                pending += if (!p.isSolved && p.pendingAttempts > 0) 1 else 0
                            }

                            return@List ProblemSolutionsStatistic(success, wrong, pending)
                        }
                    )
                }
            }.stateIn(this)
                .also { DataBus.statisticFlow.completeOrThrow(it) }
        }
    }
}