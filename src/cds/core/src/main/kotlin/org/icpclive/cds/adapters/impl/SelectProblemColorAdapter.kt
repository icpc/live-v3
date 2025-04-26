package org.icpclive.cds.adapters.impl

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.transform
import org.icpclive.cds.*
import org.icpclive.cds.api.*
import org.icpclive.cds.utils.withGroupedRuns

private fun RunInfo.shouldDiscloseColor() = (result as? RunResult.ICPC)?.verdict?.isAccepted == true && !isHidden

@OptIn(InefficientContestInfoApi::class)
private fun ContestInfo.applyColors(problems: Set<Pair<ProblemId, Boolean?>>): ContestInfo {
    val newProblemList = problemList.map {
        when (problemColorPolicy) {
            is ProblemColorPolicy.WhenSolved if (it.id to true) !in problems -> it.copy(color = problemColorPolicy.colorBeforeSolved)
            is ProblemColorPolicy.AfterStart if status is ContestStatus.BEFORE -> it.copy(color = problemColorPolicy.colorBeforeStart)
            is ProblemColorPolicy.Always, is ProblemColorPolicy.AfterStart, is ProblemColorPolicy.WhenSolved -> it
        }
    }
    return if (newProblemList != problemList) {
        this.copy(problemList = newProblemList)
    } else {
        this
    }
}

internal fun selectProblemColors(flow: Flow<ContestUpdate>): Flow<ContestUpdate> = flow.withGroupedRuns({ run: RunInfo ->
    run.problemId to run.shouldDiscloseColor()
}).transform {
    when (it.event) {
        is InfoUpdate -> emit(InfoUpdate(it.event.newInfo.applyColors(it.runs.keys)))
        is RunUpdate -> {
            val run = it.event.newInfo
            if (run.shouldDiscloseColor() && it.runs[run.problemId to true]?.size == 1) {
                val newInfo = it.infoAfterEvent!!.applyColors(it.runs.keys)
                if (newInfo != it.infoAfterEvent) {
                    emit(InfoUpdate(newInfo))
                }
            }
            emit(it.event)
        }

        is CommentaryMessagesUpdate -> emit(it.event)
    }
}
