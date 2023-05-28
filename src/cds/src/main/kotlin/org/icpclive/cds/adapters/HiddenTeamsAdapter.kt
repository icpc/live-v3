package org.icpclive.cds.adapters

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.icpclive.api.ContestInfo
import org.icpclive.cds.ContestUpdate


fun Flow<ContestUpdate>.processHiddenTeams() = withGroupedRuns(
    { it.teamId },
    { key, _, original, info ->
        val team = info?.teams?.firstOrNull { it.id == key }
        if (team?.isHidden == true)
            original.map { it.copy(isHidden = true) }
        else
            original
    },
    { new: ContestInfo, old: ContestInfo?, key: Int ->
        new.teams.firstOrNull { it.id == key }?.isHidden != old?.teams?.firstOrNull { it.id == key }?.isHidden
    }
).map { it.event }