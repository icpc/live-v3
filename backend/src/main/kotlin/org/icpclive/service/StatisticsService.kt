package org.icpclive.service

import kotlinx.coroutines.CoroutineScope
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
            }.stateIn(this)
                .also { DataBus.statisticFlow.completeOrThrow(it) }
        }
    }
}