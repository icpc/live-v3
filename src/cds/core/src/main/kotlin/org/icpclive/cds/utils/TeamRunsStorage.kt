package org.icpclive.cds.utils

import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.persistentMapOf
import org.icpclive.cds.RunUpdate
import org.icpclive.cds.api.*

public class TeamRunsStorage {
    private fun RunInfo.isTested() = !isHidden && result !is RunResult.InProgress
    private var runsByTeamId = persistentMapOf<TeamId, PersistentList<RunInfo>>()

    public fun applyEvent(state: ContestState) : List<TeamId> {
        if (state.lastEvent !is RunUpdate) return emptyList()
        val oldRun = state.runsBeforeEvent[state.lastEvent.newInfo.id]
        val newRun = state.lastEvent.newInfo
        return updateRun(oldRun, newRun)
    }

    public fun updateRun(oldRun: RunInfo?, newRun: RunInfo) : List<TeamId> {
        if (oldRun?.teamId != newRun.teamId) {
            if (oldRun != null) {
                runsByTeamId = runsByTeamId.removeRun(oldRun.teamId, oldRun)
            }
            runsByTeamId = runsByTeamId.addAndResort(newRun.teamId, newRun)
        } else {
            runsByTeamId = runsByTeamId.updateAndResort(newRun.teamId, newRun)
        }
        return listOfNotNull(oldRun?.teamId, newRun.teamId).distinct().takeIf {
            oldRun == null || oldRun.isTested() || newRun.isTested()
        } ?: emptyList()
    }

    public fun getRuns(key: TeamId) : List<RunInfo> {
        return runsByTeamId[key] ?: emptyList()
    }
}