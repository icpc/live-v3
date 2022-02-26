package org.icpclive.service

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collect
import org.icpclive.DataBus
import org.icpclive.api.ProblemSolutionsStatistic
import org.icpclive.api.RunInfo
import org.icpclive.api.SolutionsStatistic

class StatisticsService(private val problemNumber: Int, private val runsFlow: Flow<RunInfo>) {
    val result = MutableStateFlow(SolutionsStatistic(List(problemNumber) {
        ProblemSolutionsStatistic(0, 0, 0)
    })).also { DataBus.setStatisticEvents(it) }
    private val pending = List<MutableSet<Int>>(problemNumber) { mutableSetOf() }
    private val wrong = List<MutableSet<Int>>(problemNumber) { mutableSetOf() }
    private val success = List<MutableSet<Int>>(problemNumber) { mutableSetOf() }
    private val runToProblem = mutableMapOf<Int, Int>()

    private fun recalc() {
        result.value = SolutionsStatistic(
            List(problemNumber) {
                ProblemSolutionsStatistic(
                    success[it].size,
                    wrong[it].size,
                    pending[it].size
                )
            }
        )
    }

    suspend fun run() {
        runsFlow.collect {
            runToProblem[it.id]?.let { problem ->
                pending[problem].remove(it.id)
                wrong[problem].remove(it.id)
                success[problem].remove(it.id)
            }
            when {
                it.isAccepted -> success[it.problemId]
                !it.isJudged -> pending[it.problemId]
                else -> wrong[it.problemId]
            }.add(it.id)
            runToProblem[it.id] = it.problemId
            recalc()
        }
    }
}