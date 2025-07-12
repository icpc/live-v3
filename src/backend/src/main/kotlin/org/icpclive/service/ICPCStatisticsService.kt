package org.icpclive.service

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import org.icpclive.api.*
import org.icpclive.cds.api.*
import org.icpclive.cds.scoreboard.ContestStateWithScoreboard
import org.icpclive.cds.util.completeOrThrow
import org.icpclive.data.DataBus
import kotlin.time.Duration.Companion.milliseconds

class StatisticsService : Service {
    override fun CoroutineScope.runOn(flow: Flow<ContestStateWithScoreboard>) {
        flow.conflate().mapNotNull {
            delay(100.milliseconds)
            when (it.state.infoAfterEvent?.resultType) {
                ContestResultType.IOI -> computeIOIStatistics(it)
                ContestResultType.ICPC -> computeICPCStatistics(it)
                null -> null
            }
        }
            .stateIn(this, SharingStarted.Eagerly, ICPCSolutionsStatistic(0, emptyList()))
            .also { DataBus.statisticFlow.completeOrThrow(it) }
    }

    private fun computeICPCStatistics(it: ContestStateWithScoreboard): SolutionsStatistic? {
        val info = it.state.infoAfterEvent ?: return null
        val problems = info.scoreboardProblems.size
        return ICPCSolutionsStatistic(
            it.rankingAfter.order.size,
            List(problems) { problemId ->
                var success = 0
                var wrong = 0
                var pending = 0

                for (teamId in it.rankingAfter.order) {
                    val row = it.scoreboardRowAfterOrNull(teamId) ?: continue
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

    private fun computeIOIStatistics(it: ContestStateWithScoreboard): SolutionsStatistic? {
        val info = it.state.infoAfterEvent ?: return null
        val problems = info.scoreboardProblems.size
        return IOISolutionsStatistic(
            it.rankingAfter.order.size,
            List(problems) { problemId ->
                val listScore = mutableListOf<Double>()
                var pending = 0
                for (teamId in it.rankingAfter.order) {
                    val row = it.scoreboardRowAfterOrNull(teamId) ?: continue
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
    }

}
