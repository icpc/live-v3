package org.icpclive.cds.adapters

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.icpclive.cds.api.ContestInfo
import org.icpclive.cds.ContestUpdate
import org.icpclive.cds.api.ProblemId

public fun Flow<ContestUpdate>.processHiddenProblems(): Flow<ContestUpdate> =
    withGroupedRuns(
        { it.problemId },
        { key, _, original, info ->
            val problem = info?.problems?.get(key)
            if (problem?.isHidden != false)
                original.map { it.copy(isHidden = true) }
            else
                original
        },
        { new: ContestInfo, old: ContestInfo?, key: ProblemId ->
            new.problems[key]?.isHidden != old?.problems?.get(key)?.isHidden
        }
    ).map { it.event }