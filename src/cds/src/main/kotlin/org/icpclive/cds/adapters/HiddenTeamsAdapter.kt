package org.icpclive.cds.adapters

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.icpclive.api.ContestInfo
import org.icpclive.api.TeamInfo
import org.icpclive.cds.ContestUpdate
import org.icpclive.cds.InfoUpdate

private fun TeamInfo.updateHidden(isHidden: Boolean, isOutOfContest: Boolean) = if (isHidden != this.isHidden || isOutOfContest != this.isOutOfContest) {
    copy(
        isHidden = isHidden,
        isOutOfContest = isOutOfContest
    )
} else {
    this
}

fun Flow<ContestUpdate>.processHiddenTeamsAndGroups() =
    map {
        if (it is InfoUpdate) {
            InfoUpdate(
                it.newInfo.copy(
                    teams = it.newInfo.teams.map { team ->
                        team.updateHidden(
                            isHidden = team.isHidden || team.groups.any { group -> it.newInfo.groupById(group)?.isHidden == true },
                            isOutOfContest = team.isOutOfContest || team.groups.any { group -> it.newInfo.groupById(group)?.isOutOfContest == true },
                        )
                    }
                )
            )
        } else {
            it
        }
    }.withGroupedRuns(
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