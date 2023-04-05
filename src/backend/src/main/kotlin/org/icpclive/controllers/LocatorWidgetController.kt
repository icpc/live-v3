package org.icpclive.controllers

import kotlinx.coroutines.flow.first
import org.icpclive.admin.ApiActionException
import org.icpclive.api.*
import org.icpclive.data.DataBus
import org.icpclive.data.Manager

class LocatorWidgetController(manager: Manager<TeamLocatorWidget>) :
    SingleWidgetController<ExternalTeamLocatorSettings, TeamLocatorWidget>(ExternalTeamLocatorSettings(), manager) {
    override suspend fun onDelete(id: Int) {}

    fun TeamLocatorExternalCircleSettings.getTeam(teams: List<TeamInfo>) =
        teams.singleOrNull { it.id == teamId } ?: teams.singleOrNull { it.contestSystemId == cdsTeamId }

    override suspend fun checkSettings(settings: ExternalTeamLocatorSettings) {
        val teams = DataBus.contestInfoFlow.await().first().teams
        settings.circles.forEach { it.getTeam(teams) ?: throw ApiActionException("No team for circle $it") }
    }

    override suspend fun constructWidget(settings: ExternalTeamLocatorSettings): TeamLocatorWidget {
        checkSettings(settings)
        val teams = DataBus.contestInfoFlow.await().first().teams
        fun TeamLocatorExternalCircleSettings.toCircle() = TeamLocatorCircleSettings(
            x, y, radius, getTeam(teams)?.id ?: throw ApiActionException("No team for circle $this")
        )
        return TeamLocatorWidget(TeamLocatorSettings(settings.circles.map { it.toCircle() }, settings.scene))
    }
}