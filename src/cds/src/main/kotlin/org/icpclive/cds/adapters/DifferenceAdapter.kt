package org.icpclive.cds.adapters

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import org.icpclive.api.*
import org.icpclive.cds.ContestDataSource
import org.icpclive.util.completeOrThrow
import org.icpclive.util.getLogger
import kotlin.math.abs

class DifferenceAdapter(private val source: ContestDataSource) : ContestDataSource {
    private val runMap = mutableMapOf<Pair<Int, Int>, MutableList<RunInfo>>()

    private suspend fun FlowCollector<RunInfo>.add(run: RunInfo) {
        val runsList = runMap
            .getOrPut(run.teamId to run.problemId) { mutableListOf() }
        val index = runsList.indexOfFirst { it.id == run.id }
        if (run.result == null) {
            emit(run)
            if (index != -1) runsList.removeAt(index) else return
        } else {
            val copy = run.copy(result = (run.result as IOIRunResult).copy(difference = Double.NaN))
            if (index == -1) runsList.add(copy) else runsList[index] = copy
        }
        recalc(runsList)
    }
    private suspend fun FlowCollector<RunInfo>.recalc(list: MutableList<RunInfo>) {
        list.sortBy { it.id }
        val bestByGroup = mutableMapOf<Int, Double>()

        for ((index, i) in list.withIndex()) {
            val byGroup = (i.result as IOIRunResult).scoreByGroup
            var d = 0.0
            for (g in byGroup.indices) {
                if (bestByGroup.getOrDefault(g, 0.0) < byGroup[g]) {
                    d += byGroup[g] - bestByGroup.getOrDefault(g, 0.0)
                    bestByGroup[g] = byGroup[g]
                }
            }
            if (i.result.difference.isNaN() || abs(i.result.difference - d) > 1e-5) {
                list[index] = i.copy(result = i.result.copy(difference = d))
                emit(list[index])
            }
        }
    }

    override suspend fun run(
        contestInfoDeferred: CompletableDeferred<StateFlow<ContestInfo>>,
        runsDeferred: CompletableDeferred<Flow<RunInfo>>,
        analyticsMessagesDeferred: CompletableDeferred<Flow<AnalyticsMessage>>
    ) {
        coroutineScope {
            val unprocessedRunsDeferred = CompletableDeferred<Flow<RunInfo>>()
            launch { source.run(contestInfoDeferred, unprocessedRunsDeferred, analyticsMessagesDeferred) }
            logger.info("Difference service is started")
            if (contestInfoDeferred.await().value.resultType == ContestResultType.ICPC) {
                runsDeferred.completeOrThrow(unprocessedRunsDeferred.await())
            } else {
                runsDeferred.completeOrThrow(
                    flow {
                        unprocessedRunsDeferred.await().collect {
                            add(it)
                        }
                    }.shareIn(this, SharingStarted.Eagerly, replay = Int.MAX_VALUE)
                )
            }
        }
    }

    companion object {
        private val logger = getLogger(DifferenceAdapter::class)
    }
}