package org.icpclive.admin

import kotlinx.coroutines.flow.first
import org.icpclive.api.InefficientContestInfoApi
import org.icpclive.data.DataBus

@OptIn(InefficientContestInfoApi::class)
suspend fun getTeams() = DataBus.contestInfoFlow.await().first().teamList.filterNot { it.isHidden }

suspend fun getRegions() = getTeams().flatMap { it.groups }.distinct().sorted()

suspend fun getHashtags() = getTeams().filter { it.hashTag != null }.associateBy({ it.hashTag!! }, { it.id })