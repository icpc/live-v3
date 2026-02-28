package org.icpclive.controllers

import org.icpclive.api.*
import org.icpclive.cds.api.ContestInfo
import org.icpclive.data.*
import org.icpclive.server.ApiActionException

class LocatorWidgetController(manager: Manager<in TeamLocatorWidget>) :
    SingleWidgetController<ExternalTeamLocatorSettings, TeamLocatorWidget>(ExternalTeamLocatorSettings(), manager) {
    override suspend fun onDelete(id: Int) {}

    private fun TeamLocatorExternalCircleSettings.getTeam(info: ContestInfo) = info.teams[teamId]

    override suspend fun checkSettings(settings: ExternalTeamLocatorSettings) {
        val info = DataBus.currentContestInfo()
        settings.circles.forEach {
            val _ = it.getTeam(info) ?: throw ApiActionException("No team for circle $it")
        }
    }

    override suspend fun constructWidget(settings: ExternalTeamLocatorSettings): TeamLocatorWidget {
        checkSettings(settings)
        val info = DataBus.currentContestInfo()
        fun TeamLocatorExternalCircleSettings.toCircle() = TeamLocatorCircleSettings(
            x, y, radius, getTeam(info)?.id ?: throw ApiActionException("No team for circle $this")
        )
        return TeamLocatorWidget(TeamLocatorSettings(settings.circles.map { it.toCircle() }, settings.scene))
    }
}