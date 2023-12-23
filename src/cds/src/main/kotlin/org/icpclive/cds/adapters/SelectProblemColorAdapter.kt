package org.icpclive.cds.adapters

import kotlinx.coroutines.flow.*
import org.icpclive.api.*
import org.icpclive.cds.*

private fun RunInfo.shouldDiscloseColor() = (result as? ICPCRunResult)?.verdict?.isAccepted == true && !isHidden
@OptIn(InefficientContestInfoApi::class)
private fun ContestInfo.applyColors(problems: Set<Pair<Int, Boolean?>>) : ContestInfo {
    val newProblemList = problemList.map {
        if ((it.id to true) in problems || it.unsolvedColor == null) {
            it
        } else {
            it.copy(color = it.unsolvedColor)
        }
    }
    return if (newProblemList != problemList) {
        copy(
            problemList = newProblemList
        )
    } else {
        this
    }
}

public fun Flow<ContestUpdate>.selectProblemColors() : Flow<ContestUpdate> = withGroupedRuns({
    run: RunInfo -> run.problemId to run.shouldDiscloseColor()
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
        is AnalyticsUpdate -> emit(it.event)
    }
}