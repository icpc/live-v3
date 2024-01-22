package org.icpclive.admin

import kotlinx.coroutines.flow.first
import org.icpclive.cds.api.GroupInfo
import org.icpclive.cds.api.InefficientContestInfoApi
import org.icpclive.data.DataBus

@OptIn(InefficientContestInfoApi::class)
suspend fun getTeams() = DataBus.contestInfoFlow.await().first().teamList.filterNot { it.isHidden }

@OptIn(InefficientContestInfoApi::class)
suspend fun getRegions() : List<GroupInfo> {
    val info = DataBus.contestInfoFlow.await().first()
    val used = info.teamList.flatMap { it.groups }.toSet()
    return info.groupList.filter { it.cdsId in used }
}

suspend fun getHashtags() = getTeams().filter { it.hashTag != null }.associateBy({ it.hashTag!! }, { it.id })