package org.icpclive.cds.adapters

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import org.icpclive.api.*
import org.icpclive.cds.ContestDataSource
import org.icpclive.util.completeOrThrow
import org.icpclive.util.getLogger
import java.util.*

internal class IOIFirstToSolveAdapter(private val source: ContestDataSource) : ContestDataSource {
    // problem id -> Pair<RunInfo, Total Score>
    private val firstSolveMap = mutableMapOf<Int, Pair<RunInfo, Double>>()
    private val solvedMap = mutableMapOf<Int, MutableMap<Int, Set<RunInfo>>>()

    private fun RunInfo.setFTS(value: Boolean) = copy(result = (result as? IOIRunResult)?.copy(isFirstBestRun = value))

    private suspend fun FlowCollector<RunInfo>.replaceFTS(oldRun: RunInfo?, newRun: RunInfo, score: Double) {
        firstSolveMap[newRun.problemId] = Pair(newRun, score)
        oldRun?.setFTS(false)?.let { emit(it) }
        emit(newRun.setFTS(true))
    }

    override suspend fun run(
        contestInfoDeferred: CompletableDeferred<StateFlow<ContestInfo>>,
        runsDeferred: CompletableDeferred<Flow<RunInfo>>,
        analyticsMessagesDeferred: CompletableDeferred<Flow<AnalyticsMessage>>
    ) {
        coroutineScope {
            val unprocessedRunsDeferred = CompletableDeferred<Flow<RunInfo>>()
            launch { source.run(contestInfoDeferred, unprocessedRunsDeferred, analyticsMessagesDeferred) }

            val infoFlow = contestInfoDeferred.await()

            logger.info("First to solve service is started")
            runsDeferred.completeOrThrow(
                flow {
                    val modesFlow = infoFlow.map { ScoreMergeModes(it.problems) }.distinctUntilChanged()
                    var modes = ScoreMergeModes(infoFlow.value.problems)

                    merge(
                        unprocessedRunsDeferred.await(),
                        modesFlow
                    ).collect {
                        when(it) {
                            is RunInfo -> {
                                val result = it.result

                                if(result is IOIRunResult) {
                                    val accumulator = when (modes.modes[it.problemId] ?: ScoreMergeMode.LAST) {
                                        ScoreMergeMode.MAX_PER_GROUP -> MaxByGroupScoreAccumulator()
                                        ScoreMergeMode.MAX_TOTAL -> MaxTotalScoreAccumulator()
                                        ScoreMergeMode.LAST -> LastScoreAccumulator()
                                        ScoreMergeMode.LAST_OK -> LastOKScoreAccumulator()
                                        ScoreMergeMode.SUM -> SumScoreAccumulator()
                                    }
                                    val teamProblemRuns = solvedMap.getOrDefault(it.problemId, mutableMapOf()).getOrDefault(it.teamId, TreeSet())
                                    teamProblemRuns.forEach { problemRun ->
                                        accumulator.add(problemRun.result as IOIRunResult)
                                    }
                                    accumulator.add(result)

                                    val score = accumulator.total

                                    val firstSolveScore = firstSolveMap[it.problemId]
                                    var isFirstBest = false
                                    if(firstSolveScore != null) {
                                        if(score > firstSolveScore.second) {
                                            replaceFTS(firstSolveScore.first, it, score)
                                            isFirstBest = true
                                        }
                                    } else if(score > 0.0) {
                                        replaceFTS(null, it, score)
                                        isFirstBest = true
                                    }

                                    solvedMap.getOrPut(it.problemId) {
                                        val set = mutableSetOf<RunInfo>()
                                        set.add(it)

                                        mutableMapOf(it.teamId to set)
                                    }

                                    if(!isFirstBest) emit(it)
                                }
                            }

                            is ScoreMergeModes -> {
                                modes = it
                            }
                        }
                    }
                }.shareIn(this, SharingStarted.Eagerly, replay = Int.MAX_VALUE)
            )
        }
    }

    companion object {
        private val logger = getLogger(IOIFirstToSolveAdapter::class)
    }
}