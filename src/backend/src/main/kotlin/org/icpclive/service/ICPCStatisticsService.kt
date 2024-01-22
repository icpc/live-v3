package org.icpclive.service

import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.*
import org.icpclive.api.*
import org.icpclive.cds.api.ICPCProblemResult
import org.icpclive.util.completeOrThrow
import org.icpclive.data.DataBus
import org.icpclive.cds.scoreboard.ScoreboardAndContestInfo

class ICPCStatisticsService {
    suspend fun run(scoreboardFlow: Flow<ScoreboardAndContestInfo>) {
        coroutineScope {
            scoreboardFlow.conflate().map {
                val problems = it.info.scoreboardProblems.size
                SolutionsStatistic(
                    List(problems) { problemId ->
                        var success = 0
                        var wrong = 0
                        var pending = 0

                        for (row in it.scoreboardSnapshot.rows.values) {
                            val p = row.problemResults[problemId]
                            require(p is ICPCProblemResult)
                            success += if (p.isSolved) 1 else 0
                            wrong += if (!p.isSolved && p.wrongAttempts > 0 && p.pendingAttempts == 0) 1 else 0
                            pending += if (!p.isSolved && p.pendingAttempts > 0) 1 else 0
                        }

                        return@List ICPCProblemSolutionsStatistic(success, wrong, pending)
                    }
                )
            }
                .stateIn(this)
                .also { DataBus.statisticFlow.completeOrThrow(it) }
        }
    }
}