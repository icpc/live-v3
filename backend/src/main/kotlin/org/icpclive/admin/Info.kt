package org.icpclive.admin

import kotlinx.coroutines.flow.first
import org.icpclive.data.DataBus

suspend fun getTeams() = DataBus.contestInfoFlow.await().first().teams

suspend fun getRegions() = DataBus.contestInfoFlow.await().first().teams.flatMap { it.groups }.distinct().sorted()