package org.icpclive.service

import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.*
import org.icpclive.api.*
import org.icpclive.util.completeOrThrow
import org.icpclive.data.DataBus

class IOIStatisticsService {
    suspend fun run(scoreboardFlow: Flow<Scoreboard>, contestInfoFlow: Flow<ContestInfo>) {
        coroutineScope {
            combine(
                scoreboardFlow,
                contestInfoFlow.map { it.problems.size }.distinctUntilChanged(),
                ::Pair,
            ).map { (scoreboard, problemNumber) ->
                if (scoreboard.rows.isEmpty()) {
                    SolutionsStatistic(List(problemNumber) { IOIProblemSolutionsStatistic(emptyList(), 0) })
                } else {
                    SolutionsStatistic(
                        List(scoreboard.rows[0].problemResults.size) { problemId ->
                            val listScore = mutableListOf<Double>();
                            var pending = 0
                            for(row in scoreboard.rows) {
                                val p = row.problemResults[problemId]
                                require(p is IOIProblemResult)
                                if(p.score != null) {
                                    listScore.add(p.score!!)
                                } else {
                                    ++pending
                                }
                            }

                            listScore.sortDescending()

                            val entityList = mutableListOf<IOIProblemEntity>()
                            var currentCount = 0
                            var currentScore = 0.0
                            listScore.forEach {
                                if(it != currentScore) {
                                    entityList.add(IOIProblemEntity(currentCount, currentScore))

                                    currentCount = 1
                                    currentScore = it
                                } else {
                                    ++currentCount
                                }
                            }

                            entityList.add(IOIProblemEntity(currentCount, currentScore))

                            return@List IOIProblemSolutionsStatistic(entityList, pending)
                        }
                    )
                }
            }.stateIn(this)
                .also { DataBus.statisticFlow.completeOrThrow(it) }
        }
    }
}