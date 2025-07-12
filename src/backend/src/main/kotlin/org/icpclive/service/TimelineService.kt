package org.icpclive.service

import kotlinx.collections.immutable.persistentMapOf
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.*
import org.icpclive.api.TimeLineRunInfo
import org.icpclive.api.TimeLineRunInfo.*
import org.icpclive.cds.ContestUpdate
import org.icpclive.cds.adapters.contestState
import org.icpclive.cds.api.*
import org.icpclive.cds.scoreboard.ContestStateWithScoreboard
import org.icpclive.cds.utils.TeamRunsStorage
import org.icpclive.data.DataBus

internal class TimelineService : Service {
    override fun CoroutineScope.runOn(flow: Flow<ContestStateWithScoreboard>) {
        DataBus.timelineFlow.complete(flow.map { it.state.lastEvent }.timelineFlow().stateIn(this, SharingStarted.Eagerly, emptyMap()))
    }

    private fun Flow<ContestUpdate>.timelineFlow() = flow {
        var rows = persistentMapOf<TeamId, List<TimeLineRunInfo>>()
        val runsByTeamId = TeamRunsStorage()
        contestState().collect { state ->
            for (team in runsByTeamId.applyEvent(state)) {
                val newRow = runsByTeamId.getRuns(team).toTimeLine()
                val oldRow = rows[team]
                if (newRow != oldRow) { // optimization: avoid identity change, if no real change
                    rows = rows.put(team, newRow)
                }
            }
            emit(rows)
        }
    }

    private fun List<RunInfo>?.toTimeLine() : List<TimeLineRunInfo> {
        val acceptedProblems = mutableSetOf<ProblemId>()
        return (this ?: emptyList()).mapNotNull {
            when (val result = it.result) {
                is RunResult.ICPC -> {
                    if (!acceptedProblems.contains(it.problemId)) {
                        if (result.verdict.isAccepted) {
                            acceptedProblems.add(it.problemId)
                        }
                        ICPC(it.time, it.problemId, result.verdict.isAccepted, result.verdict.shortName)
                    } else {
                        null
                    }
                }

                is RunResult.IOI -> {
                    if (result.difference > 0) {
                        IOI(it.time, it.problemId, result.scoreAfter)
                    } else {
                        null
                    }
                }

                is RunResult.InProgress -> {
                    InProgress(it.time, it.problemId)
                }
            }
        }
    }


}