package org.icpclive.service

import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import org.icpclive.api.*
import org.icpclive.util.completeOrThrow
import org.icpclive.data.DataBus
import org.icpclive.scoreboard.ScoreboardAndContestInfo
import kotlin.time.Duration.Companion.milliseconds

class IOIStatisticsService {
    suspend fun run(scoreboardFlow: Flow<ScoreboardAndContestInfo>) {
        coroutineScope {
            scoreboardFlow.conflate().transform {
                emit(it)
                delay(100.milliseconds)
            }.map {
                val problems = it.info.problems.size
                SolutionsStatistic(
                    List(problems) { problemId ->
                        val listScore = mutableListOf<Double>()
                        var pending = 0
                        for (row in it.scoreboardSnapshot.rows.values) {
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