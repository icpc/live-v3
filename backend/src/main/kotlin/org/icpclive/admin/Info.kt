package org.icpclive.admin

import kotlinx.coroutines.flow.first
import org.icpclive.data.DataBus

suspend fun getTeams() = DataBus.contestInfoFlow.await().first().teams.filterNot { it.isHidden }

suspend fun getRegions() = getTeams().flatMap { it.groups }.distinct().sorted()

suspend fun getHashtags() = getTeams().filter { it.hashTag != null }.associateBy({ it.hashTag!! }, { it.id })