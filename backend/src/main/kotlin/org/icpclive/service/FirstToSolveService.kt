package org.icpclive.service

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import org.icpclive.api.RunInfo
import org.icpclive.utils.getLogger
import java.util.*

class FirstToSolveService {
    private val firstSolve = mutableMapOf<Int, RunInfo>()
    private val runComparator = compareBy<RunInfo>({it.isHidden}, { it.time }, { it.id })
    private val solved = mutableMapOf<Int, TreeSet<RunInfo>>()
    private val solvedById = mutableMapOf<Int, RunInfo>()

    private fun getSolved(problemId: Int) = solved.getOrPut(problemId) { TreeSet(runComparator) }

    private suspend fun replace(old: RunInfo, new: RunInfo, runsFlow: MutableSharedFlow<RunInfo>) {
        require(old.id == new.id)
        require(old.isAccepted && new.isAccepted)
        getSolved(old.problemId).remove(old)
        getSolved(new.problemId).add(new)
        solvedById[old.id] = new
        if (old.problemId != new.problemId) {
            logger.warn("Run ${old.id} changes problem from ${old.problemId} to ${new.problemId}")
            recalculateFTS(old.problemId, runsFlow)
        }
    }

    suspend fun run(rawRunsFlow: Flow<RunInfo>, runsFlow: MutableSharedFlow<RunInfo>) {
        rawRunsFlow.collect {
            val oldRun = solvedById[it.id]
            if (oldRun != null) {
                if (it.isAccepted) {
                    replace(solvedById[it.id]!!, it, runsFlow)
                } else {
                    solvedById.remove(oldRun.problemId)
                    getSolved(oldRun.problemId).remove(oldRun)
                    recalculateFTS(oldRun.problemId, runsFlow)
                }
            } else if (it.isAccepted) {
                getSolved(it.problemId).add(it)
                solvedById[it.id] = it
            }
            recalculateFTS(it.problemId, runsFlow)
            if (firstSolve[it.problemId]?.id != it.id) {
                runsFlow.emit(it)
            }
        }
    }

    private suspend fun recalculateFTS(problemId: Int, runsFlow: MutableSharedFlow<RunInfo>) {
        val result = if (getSolved(problemId).isEmpty()) null else getSolved(problemId).first().takeIf { !it.isHidden }
        if (firstSolve[problemId] == result) return
        firstSolve[problemId].takeIf { it?.id != result?.id }?.run {
            val newVersion = copy(isFirstSolvedRun = false)
            runsFlow.emit(newVersion)
            replace(this, newVersion, runsFlow)
            logger.warn("First to solve for problem $problemId was replaced from $id to ${result?.id}")
        }
        if (result == null) {
            firstSolve.remove(problemId)
            return
        }
        val newVersion = result.copy(isFirstSolvedRun = true)
        firstSolve[problemId] = newVersion
        replace(result, newVersion, runsFlow)
        runsFlow.emit(newVersion)
    }

    companion object {
        private val logger = getLogger(FirstToSolveService::class)
    }
}