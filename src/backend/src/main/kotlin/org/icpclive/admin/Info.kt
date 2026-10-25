package org.icpclive.admin

import kotlinx.coroutines.flow.first
import org.icpclive.cds.api.GroupInfo
import org.icpclive.cds.api.InefficientContestInfoApi
import org.icpclive.data.DataBus

@OptIn(InefficientContestInfoApi::class)
context(dataBus: DataBus)
suspend fun getTeams() = dataBus.contestInfoFlow.first().teamList.filterNot { it.isHidden }

@OptIn(InefficientContestInfoApi::class)
context(dataBus: DataBus)
suspend fun getRegions(): List<GroupInfo> {
    val info = dataBus.contestInfoFlow.first()
    val used = info.teamList.flatMap { it.groups }.toSet()
    return info.groupList.filter { it.id in used }
}

context(dataBus: DataBus)
suspend fun getHashtags() = getTeams().filter { it.hashTag != null }.associateBy({ it.hashTag!! }, { it.id })

