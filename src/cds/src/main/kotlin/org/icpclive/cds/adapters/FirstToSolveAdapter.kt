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
import java.util.*

class FirstToSolveAdapter(private val source: ContestDataSource) : ContestDataSource {
    private val firstSolve = mutableMapOf<Int, RunInfo>()
    private val runComparator = compareBy<RunInfo>({it.isHidden}, { it.time }, { it.id })
    private val solved = mutableMapOf<Int, TreeSet<RunInfo>>()
    private val solvedById = mutableMapOf<Int, RunInfo>()

    private fun getSolved(problemId: Int) = solved.getOrPut(problemId) { TreeSet(runComparator) }

    private suspend fun FlowCollector<RunInfo>.replace(old: RunInfo, new: RunInfo) {
        require(old.id == new.id)
        require(old.isAccepted && new.isAccepted)
        getSolved(old.problemId).remove(old)
        getSolved(new.problemId).add(new)
        solvedById[old.id] = new
        if (old.problemId != new.problemId) {
            logger.warn("Run ${old.id} changes problem from ${old.problemId} to ${new.problemId}")
            recalculateFTS(old.problemId)
        }
    }

    override suspend fun loadOnce() = source.loadOnce().let {
        it.runs.forEach { run -> getSolved(run.problemId).add(run) }
        solved.keys.forEach { problemId -> FlowCollector<RunInfo>{}.recalculateFTS(problemId) }
        ContestParseResult(
            it.contestInfo,
            it.runs.map { run -> run.copy(isFirstSolvedRun = firstSolve[run.problemId] == run) },
            it.analyticsMessages
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
            logger.info("First to solve service is started")
            flow {
                unprocessedRunsDeferred.await().collect {
                    val oldRun = solvedById[it.id]
                    if (oldRun != null) {
                        if (it.isAccepted) {
                            replace(solvedById[it.id]!!, it)
                        } else {
                            solvedById.remove(oldRun.problemId)
                            getSolved(oldRun.problemId).remove(oldRun)
                            recalculateFTS(oldRun.problemId)
                        }
                    } else if (it.isAccepted) {
                        getSolved(it.problemId).add(it)
                        solvedById[it.id] = it
                    }
                    recalculateFTS(it.problemId)
                    if (firstSolve[it.problemId]?.id != it.id) {
                        emit(it)
                    }
                }
            }.shareIn(this, SharingStarted.Eagerly, replay = Int.MAX_VALUE)
                .also { runsDeferred.complete(it) }
        }
    }

    private suspend fun FlowCollector<RunInfo>.recalculateFTS(problemId: Int) {
        val result = if (getSolved(problemId).isEmpty()) null else getSolved(problemId).first().takeIf { !it.isHidden }
        if (firstSolve[problemId] == result) return
        firstSolve[problemId]?.takeIf { it.id != result?.id }?.run {
            val newVersion = copy(isFirstSolvedRun = false)
            emit(newVersion)
            replace(this, newVersion)
            logger.warn("First to solve for problem $problemId was replaced from $id to ${result?.id}")
        }
        if (result == null) {
            firstSolve.remove(problemId)
            return
        }
        val newVersion = result.copy(isFirstSolvedRun = true)
        firstSolve[problemId] = newVersion
        replace(result, newVersion)
        emit(newVersion)
    }

    companion object {
        private val logger = getLogger(FirstToSolveAdapter::class)
    }
}