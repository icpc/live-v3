package org.icpclive.cds.adapters

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import org.icpclive.api.AnalyticsMessage
import org.icpclive.api.ContestInfo
import org.icpclive.api.RunInfo
import org.icpclive.cds.ContestDataSource
import org.icpclive.cds.ContestParseResult
import org.icpclive.util.getLogger
import java.util.TreeSet

class DifferenceAdapter(private val source: ContestDataSource) : ContestDataSource {
    private val runComparator = compareBy<RunInfo> { it.score }
    private val runMap = mutableMapOf<Int, MutableMap<Int, TreeSet<RunInfo>>>()

    private fun calculateDifference(run: RunInfo) : Float {
        val runSet = runMap
            .getOrPut(run.teamId) { mutableMapOf() }
            .getOrPut(run.problemId) { TreeSet(runComparator) }
        val last = try { runSet.last() } catch (e: NoSuchElementException) { null }
        runSet.add(run)

        return if(last != null) run.score - last.score else run.score
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
            flow {
                unprocessedRunsDeferred.await().collect {
                    emit(it.copy(difference = calculateDifference(it)))
                }
            }.shareIn(this, SharingStarted.Eagerly, replay = Int.MAX_VALUE)
                .also { runsDeferred.complete(it) }
        }
    }

    override suspend fun loadOnce() = source.loadOnce().let {
        ContestParseResult(
            it.contestInfo,
            it.runs.map { run -> run.copy(difference = calculateDifference(run)) },
            it.analyticsMessages
        )
    }

    companion object {
        private val logger = getLogger(DifferenceAdapter::class)
    }
}