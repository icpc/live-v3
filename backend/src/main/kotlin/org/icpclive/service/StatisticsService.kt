package org.icpclive.service

import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.*
import org.icpclive.api.*
import org.icpclive.util.completeOrThrow
import org.icpclive.data.DataBus

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
                            val results = buildList {
                                for(row in scoreboard.rows) {
                                    when(row.problemResults[problem]) {
                                        is ICPCProblemResult -> add(row.problemResults[problem] as ICPCProblemResult)
                                        is ScoreProblemResult -> add(row.problemResults[problem] as ScoreProblemResult)
                                    }
                                }
                            }

                            if(results[0] is ICPCProblemResult) {
                                val resultsCast = results.filterIsInstance<ICPCProblemResult>()
                                return@List ProblemSolutionsStatistic(
                                    resultsCast.count { it.isSolved },
                                    resultsCast.count { !it.isSolved && it.wrongAttempts > 0 && it.pendingAttempts == 0 },
                                    resultsCast.count { !it.isSolved && it.pendingAttempts > 0 },
                                )
                            } else if(results[0] is ScoreProblemResult) {
                                val resultsCast = results.filterIsInstance<ScoreProblemResult>()
                                return@List ProblemSolutionsStatistic(
                                    resultsCast.count { it.score > 0 },
                                    resultsCast.count { it.score == 0 && it.wrongAttempts > 0 && it.pendingAttempts == 0 },
                                    resultsCast.count { it.score == 0 && it.pendingAttempts > 0 },
                                )
                            }
                            return@List ProblemSolutionsStatistic(0, 0, 0)
                        }
                    )
                }
            }.stateIn(this)
                .also { DataBus.statisticFlow.completeOrThrow(it) }
        }
    }
}