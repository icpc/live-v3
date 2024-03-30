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

suspend fun getExternalRun(id: RunId) = DataBus.externalRunsFlow.await().first()[id]