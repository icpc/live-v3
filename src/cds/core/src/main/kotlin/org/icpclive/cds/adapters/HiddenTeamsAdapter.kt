@file:JvmMultifileClass
@file:JvmName("Adapters")

package org.icpclive.cds.adapters

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.icpclive.api.ContestInfo
import org.icpclive.api.InefficientContestInfoApi
import org.icpclive.api.TeamInfo
import org.icpclive.cds.ContestUpdate
import org.icpclive.cds.InfoUpdate

private fun TeamInfo.updateHidden(isHidden: Boolean, isOutOfContest: Boolean) =
    if (isHidden != this.isHidden || isOutOfContest != this.isOutOfContest) {
        copy(
            isHidden = isHidden,
            isOutOfContest = isOutOfContest
        )
    } else {
        this
    }


public fun Flow<ContestUpdate>.processHiddenTeamsAndGroups(): Flow<ContestUpdate> =
    map {
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
    }.withGroupedRuns(
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