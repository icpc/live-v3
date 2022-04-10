package org.icpclive.service

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import org.icpclive.api.RunInfo
import org.icpclive.utils.getLogger
import java.util.*

class FirstToSolveService(
    problemsCount: Int,
    private val rawRunsFlow: Flow<RunInfo>,
    private val runsFlow: MutableSharedFlow<RunInfo>
) {
    private val firstSolve = Array<RunInfo?>(problemsCount) { null }
    private val runComparator = compareBy<RunInfo>({ it.time }, { it.id })
    private val solved = Array(problemsCount) { TreeSet(runComparator) }
    private val solvedById = mutableMapOf<Int, RunInfo>()

    private suspend fun replace(old: RunInfo, new: RunInfo) {
        require(old.id == new.id)
        require(old.isAccepted && new.isAccepted)
        solved[old.problemId].remove(old)
        solved[new.problemId].add(new)
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
                    solved[oldRun.problemId].remove(oldRun)
                    recalculateFTS(oldRun.problemId)
                }
            } else if (it.isAccepted) {
                solved[it.problemId].add(it)
                solvedById[it.id] = it
            }
            recalculateFTS(it.problemId)
            if (firstSolve[it.problemId]?.id != it.id) {
                runsFlow.emit(it)
            }
        }
    }

    private suspend fun recalculateFTS(problemId: Int) {
        val result = if (solved[problemId].isEmpty()) null else solved[problemId].first()
        if (firstSolve[problemId] == result) return
        firstSolve[problemId].takeIf { it?.id != result?.id }?.run {
            val newVersion = copy(isFirstSolvedRun = false)
            runsFlow.emit(newVersion)
            replace(this, newVersion)
            logger.warn("First to solve for problem $problemId was replaced from $id to ${result?.id}")
        }
        if (result == null) {
            firstSolve[problemId] = null
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