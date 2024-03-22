package org.icpclive.service

import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import org.icpclive.api.*
import org.icpclive.cds.api.IOIProblemResult
import org.icpclive.util.completeOrThrow
import org.icpclive.data.DataBus
import org.icpclive.cds.scoreboard.ContestStateWithScoreboard
import kotlin.time.Duration.Companion.milliseconds

class IOIStatisticsService {
    suspend fun run(scoreboardFlow: Flow<ContestStateWithScoreboard>) {
        coroutineScope {
            scoreboardFlow.conflate().transform {
                emit(it)
                delay(100.milliseconds)
            }.mapNotNull {
                val info = it.state.infoAfterEvent ?: return@mapNotNull null
                val problems = info.problems.size
                SolutionsStatistic(
                    List(problems) { problemId ->
                        val listScore = mutableListOf<Double>()
                        var pending = 0
                        for (row in it.scoreboardRows.values) {
                            val p = row.problemResults[problemId]
                            require(p is IOIProblemResult)
                            if (p.score != null) {
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
                            if (it != currentScore) {
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
            }.stateIn(this)
                .also { DataBus.statisticFlow.completeOrThrow(it) }
        }
    }
}