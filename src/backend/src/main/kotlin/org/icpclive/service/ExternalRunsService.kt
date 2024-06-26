package org.icpclive.service

import kotlinx.collections.immutable.persistentMapOf
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.*
import org.icpclive.api.ExternalRunInfo
import org.icpclive.api.ExternalTeamInfo
import org.icpclive.cds.*
import org.icpclive.cds.api.*
import org.icpclive.cds.scoreboard.ContestStateWithScoreboard
import org.icpclive.cds.scoreboard.Ranking
import org.icpclive.data.DataBus

class ExternalRunsService : Service {
    override fun CoroutineScope.runOn(flow: Flow<ContestStateWithScoreboard>) {
        var runs = persistentMapOf<RunId, ExternalRunInfo>()
        DataBus.externalRunsFlow.complete(flow.transform {
            when (val event = it.state.lastEvent) {
                is AnalyticsUpdate -> {}
                is InfoUpdate -> {}
                is RunUpdate -> {
                    val externalInfo = event.newInfo.toExternal(it)
                    if (externalInfo == null) {
                        runs = runs.remove(event.newInfo.id)
                    } else {
                        runs = runs.put(event.newInfo.id, externalInfo)
                    }
                    emit(runs)
                }
            }
        }.stateIn(this, SharingStarted.Eagerly, emptyMap()))
    }
}

private fun RunInfo.toExternal(contestState: ContestStateWithScoreboard): ExternalRunInfo? {
    if (isHidden) return null
    val info = contestState.state.infoAfterEvent ?: return null
    return ExternalRunInfo(
        id = id.value,
        result = result,
        problem = info.problems[problemId] ?: return null,
        team = info.teams[teamId]?.toExternal(contestState) ?: return null,
        time = time,
        testedTime = testedTime,
        featuredRunMedia = featuredRunMedia,
        reactionVideos = reactionVideos,
    )
}

private fun Ranking.getTeamRank(id: TeamId) : Int? {
    val listId = order.indexOfFirst { id == it }
    if (listId == -1) return null
    return ranks[listId]
}

private fun TeamInfo.toExternal(contestState: ContestStateWithScoreboard) : ExternalTeamInfo? {
    if (isHidden) return null
    val contestInfo = contestState.state.infoAfterEvent ?: return null
    return ExternalTeamInfo(
        id = id.value,
        fullName = fullName,
        displayName = displayName,
        groups = groups.mapNotNull { contestInfo.groups[it] },
        hashTag = hashTag,
        medias = medias,
        isOutOfContest = isOutOfContest,
        organization = organizationId?.let { contestInfo.organizations[organizationId] },
        customFields = customFields,
        scoreboardRowBefore = contestState.scoreboardRowBeforeOrNull(id) ?: return null,
        rankBefore = contestState.rankingBefore.getTeamRank(id) ?: return null,
        scoreboardRowAfter = contestState.scoreboardRowAfterOrNull(id) ?: return null,
        rankAfter = contestState.rankingAfter.getTeamRank(id) ?: return null
    )
}