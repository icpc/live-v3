package org.icpclive.cds.adapters.impl

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.icpclive.cds.api.ContestInfo
import org.icpclive.cds.ContestUpdate
import org.icpclive.cds.api.ProblemId
import org.icpclive.cds.utils.withGroupedRuns

internal fun hideHiddenProblemsRuns(flow: Flow<ContestUpdate>): Flow<ContestUpdate> =
    flow.withGroupedRuns(
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