package org.icpclive.controllers

import kotlinx.coroutines.flow.first
import org.icpclive.admin.ApiActionException
import org.icpclive.api.*
import org.icpclive.cds.api.ContestInfo
import org.icpclive.data.DataBus
import org.icpclive.data.Manager

class LocatorWidgetController(manager: Manager<TeamLocatorWidget>) :
    SingleWidgetController<ExternalTeamLocatorSettings, TeamLocatorWidget>(ExternalTeamLocatorSettings(), manager) {
    override suspend fun onDelete(id: Int) {}

    fun TeamLocatorExternalCircleSettings.getTeam(info: ContestInfo) = info.teams[teamId]

    override suspend fun checkSettings(settings: ExternalTeamLocatorSettings) {
        val info = DataBus.contestInfoFlow.await().first()
        settings.circles.forEach {
            if ((it.teamId == null) == (it.cdsTeamId == null)) throw ApiActionException("Only one of of teamId and cdsTeamsId can be specified")
            it.getTeam(info) ?: throw ApiActionException("No team for circle $it")
        }
    }

    override suspend fun constructWidget(settings: ExternalTeamLocatorSettings): TeamLocatorWidget {
        checkSettings(settings)
        val info = DataBus.contestInfoFlow.await().first()
        fun TeamLocatorExternalCircleSettings.toCircle() = TeamLocatorCircleSettings(
            x, y, radius, getTeam(info)?.id ?: throw ApiActionException("No team for circle $this")
        )
        return TeamLocatorWidget(TeamLocatorSettings(settings.circles.map { it.toCircle() }, settings.scene))
    }
}