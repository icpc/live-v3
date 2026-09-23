package org.icpclive.admin

import org.icpclive.cds.api.GroupInfo
import org.icpclive.cds.api.InefficientContestInfoApi
import org.icpclive.data.DataBus
import org.icpclive.data.currentContestInfo

@OptIn(InefficientContestInfoApi::class)
context(dataBus: DataBus)
suspend fun getTeams() = dataBus.currentContestInfo().teamList.filterNot { it.isHidden }

@OptIn(InefficientContestInfoApi::class)
context(dataBus: DataBus)
suspend fun getRegions(): List<GroupInfo> {
    val info = dataBus.currentContestInfo()
    val used = info.teamList.flatMap { it.groups }.toSet()
    return info.groupList.filter { it.id in used }
}

context(dataBus: DataBus)
suspend fun getHashtags() = getTeams().filter { it.hashTag != null }.associateBy({ it.hashTag!! }, { it.id })

