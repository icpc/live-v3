package org.icpclive.admin

import kotlinx.coroutines.flow.first
import org.icpclive.api.ExternalRunInfo
import org.icpclive.api.ExternalTeamInfo
import org.icpclive.cds.api.*
import org.icpclive.data.DataBus

@OptIn(InefficientContestInfoApi::class)
suspend fun getTeams() = DataBus.contestInfoFlow.await().first().teamList.filterNot { it.isHidden }

@OptIn(InefficientContestInfoApi::class)
suspend fun getRegions() : List<GroupInfo> {
    val info = DataBus.contestInfoFlow.await().first()
    val used = info.teamList.flatMap { it.groups }.toSet()
    return info.groupList.filter { it.id in used }
}

suspend fun getHashtags() = getTeams().filter { it.hashTag != null }.associateBy({ it.hashTag!! }, { it.id })

suspend fun getExternalRun(id: Int) : ExternalRunInfo? {
    val state = DataBus.contestStateFlow.await().first()
    val contestInfo = state.infoAfterEvent ?: return null
    val runInfo = state.runs[id] ?: return null
    return runInfo.toExternal(contestInfo)
}

private fun RunInfo.toExternal(contestInfo: ContestInfo): ExternalRunInfo? {
    if (isHidden) return null
    return ExternalRunInfo(
        id = id,
        result = result,
        problem = contestInfo.problems[problemId] ?: return null,
        team = contestInfo.teams[teamId]?.toExternal(contestInfo) ?: return null,
        time = time,
        featuredRunMedia = featuredRunMedia,
        reactionVideos = reactionVideos,
    )
}

private fun TeamInfo.toExternal(contestInfo: ContestInfo) : ExternalTeamInfo? {
    if (isHidden) return null
    return ExternalTeamInfo(
        id = id.value,
        fullName = fullName,
        displayName = displayName,
        groups = groups.mapNotNull { contestInfo.groups[it] },
        hashTag = hashTag,
        medias = medias,
        isOutOfContest = isOutOfContest,
        organization = organizationId?.let { contestInfo.organizations[organizationId] },
        customFields = customFields
    )
}