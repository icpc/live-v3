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

interface ScoreAccumulator {
    fun add(score: IOIRunResult)
    val total : Double
}

class MaxByGroupScoreAccumulator : ScoreAccumulator {
    private val bestByGroup = mutableMapOf<Int, Double>()
    override var total = 0.0

    override fun add(score: IOIRunResult) {
        val byGroup = score.score
        for (g in byGroup.indices) {
            if (bestByGroup.getOrDefault(g, 0.0) < byGroup[g]) {
                total += byGroup[g] - bestByGroup.getOrDefault(g, 0.0)
                bestByGroup[g] = byGroup[g]
            }
        }
    }
}

class MaxTotalScoreAccumulator : ScoreAccumulator {
    override var total = 0.0

    override fun add(score: IOIRunResult) { total = maxOf(total, score.score.sum()) }
}

class LastScoreAccumulator : ScoreAccumulator {
    override var total = 0.0

    override fun add(score: IOIRunResult) { total = score.score.sum() }
}

class LastOKScoreAccumulator : ScoreAccumulator {
    override var total = 0.0

    override fun add(score: IOIRunResult) { if (score.wrongVerdict == null) total = score.score.sum() }
}

class SumScoreAccumulator : ScoreAccumulator {
    override var total = 0.0

    override fun add(score: IOIRunResult) { total += score.score.sum() }
}



class DifferenceAdapter(private val source: ContestDataSource) : ContestDataSource {
    private val runMap = mutableMapOf<Pair<Int, Int>, MutableList<RunInfo>>()

    private suspend fun FlowCollector<RunInfo>.add(run: RunInfo, modes: Map<Int, ScoreMergeMode>) {
        val runsList = runMap.getOrPut(run.teamId to run.problemId) { mutableListOf() }
        val index = runsList.indexOfFirst { it.id == run.id }
        if (run.result == null) {
            emit(run)
            if (index != -1) runsList.removeAt(index) else return
        } else {
            val copy = run.copy(result = (run.result as IOIRunResult).copy(difference = Double.NaN))
            if (index == -1) runsList.add(copy) else runsList[index] = copy
        }
        recalc(runsList, modes[run.problemId])
    }

    private suspend fun FlowCollector<RunInfo>.recalc(list: MutableList<RunInfo>, mergeMode: ScoreMergeMode?) {
        list.sortWith(compareBy({it.time}, { it.id }))
        val accumulator = when (mergeMode ?: ScoreMergeMode.LAST) {
            ScoreMergeMode.MAX_PER_GROUP -> MaxByGroupScoreAccumulator()
            ScoreMergeMode.MAX_TOTAL -> MaxTotalScoreAccumulator()
            ScoreMergeMode.LAST -> LastScoreAccumulator()
            ScoreMergeMode.LAST_OK -> LastOKScoreAccumulator()
            ScoreMergeMode.SUM -> SumScoreAccumulator()
        }

        for ((index, i) in list.withIndex()) {
            val before = accumulator.total
            accumulator.add(i.result as IOIRunResult)
            val after = accumulator.total

            val d = after - before
            if (i.result.difference.isNaN() || abs(i.result.difference - d) > 1e-5) {
                list[index] = i.copy(result = i.result.copy(difference = d))
                emit(list[index])
            }
        }
    }

    data class ScoreMergeModes(val modes: Map<Int, ScoreMergeMode>) {
        constructor(problems: List<ProblemInfo>): this(
            problems.associate { it.id to it.scoreMergeMode!! }
        )
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
            val infoFlow = contestInfoDeferred.await()
            if (infoFlow.value.resultType == ContestResultType.ICPC) {
                runsDeferred.completeOrThrow(unprocessedRunsDeferred.await())
            } else {
                runsDeferred.completeOrThrow(
                    flow {
                        val modesFlow = infoFlow.map { ScoreMergeModes(it.problems) }.distinctUntilChanged()
                        var modes = ScoreMergeModes(infoFlow.value.problems)
                        merge(
                            unprocessedRunsDeferred.await(),
                            modesFlow,
                        ).collect {
                            when (it) {
                                is RunInfo -> add(it, modes.modes)

                                is ScoreMergeModes -> {
                                    modes = it
                                    for ((key, list) in runMap) {
                                        recalc(list, modes.modes[key.second])
                                    }
                                }

                                else -> TODO()
                            }
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