package org.icpclive.cds.adapters.impl

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.icpclive.cds.ContestUpdate
import org.icpclive.cds.InfoUpdate
import org.icpclive.cds.api.*
import org.icpclive.cds.utils.withGroupedRuns

private fun TeamInfo.updateHidden(isHidden: Boolean, isOutOfContest: Boolean) =
    if (isHidden != this.isHidden || isOutOfContest != this.isOutOfContest) {
        copy(
            isHidden = isHidden,
            isOutOfContest = isOutOfContest
        )
    } else {
        this
    }

internal fun hideHiddenGroupsTeams(flow: Flow<ContestUpdate>) = flow.map {
    if (it is InfoUpdate) {
        InfoUpdate(
            it.newInfo.copy(
                teamList = @OptIn(InefficientContestInfoApi::class) it.newInfo.teamList.map { team ->
                    team.updateHidden(
                        isHidden = team.isHidden || team.groups.any { group -> it.newInfo.groups[group]?.isHidden == true },
                        isOutOfContest = team.isOutOfContest || team.groups.any { group -> it.newInfo.groups[group]?.isOutOfContest == true },
                    )
                }
            )
        )
    } else {
        it
    }
}

internal fun hideHiddenTeamsRuns(flow: Flow<ContestUpdate>): Flow<ContestUpdate> =
    flow.withGroupedRuns(
        { it.teamId },
        { key, _, original, info ->
            val team = info?.teams?.get(key)
            if (team?.isHidden != false)
                original.map { it.copy(isHidden = true) }
            else
                original
        },
        { new: ContestInfo, old: ContestInfo?, key: TeamId ->
            new.teams[key]?.isHidden != old?.teams?.get(key)?.isHidden
        }
    ).map { it.event }