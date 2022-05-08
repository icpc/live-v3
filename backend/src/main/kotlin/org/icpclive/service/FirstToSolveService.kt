package org.icpclive.service

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import org.icpclive.api.RunInfo
import org.icpclive.utils.getLogger
import java.util.*

class FirstToSolveService(
    private val rawRunsFlow: Flow<RunInfo>,
    private val runsFlow: MutableSharedFlow<RunInfo>
) {
    private val firstSolve = mutableMapOf<Int, RunInfo>()
    private val runComparator = compareBy<RunInfo>({ it.time }, { it.id })
    private val solved = mutableMapOf<Int, TreeSet<RunInfo>>()
    private val solvedById = mutableMapOf<Int, RunInfo>()

    fun getSolved(problemId: Int) = solved.getOrPut(problemId) { TreeSet(runComparator) }

    private suspend fun replace(old: RunInfo, new: RunInfo) {
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

    suspend fun run() {
        rawRunsFlow.collect {
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
                runsFlow.emit(it)
            }
        }
    }

    private suspend fun recalculateFTS(problemId: Int) {
        val result = if (getSolved(problemId).isEmpty()) null else getSolved(problemId).first()
        if (firstSolve[problemId] == result) return
        firstSolve[problemId].takeIf { it?.id != result?.id }?.run {
            val newVersion = copy(isFirstSolvedRun = false)
            runsFlow.emit(newVersion)
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
        runsFlow.emit(newVersion)
    }

    companion object {
        private val logger = getLogger(FirstToSolveService::class)
    }
}