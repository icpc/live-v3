package org.icpclive.controllers

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import org.icpclive.api.*
import org.icpclive.data.DataBus
import org.icpclive.data.Manager
import kotlin.time.Duration.Companion.seconds

class TeamViewController(val manager: Manager<TeamViewWidget>, val position: TeamViewPosition) :
    SingleWidgetController<ExternalTeamViewSettings, TeamViewWidget>(
        ExternalTeamViewSettings(),
        manager,
        { TeamViewWidget(OverlayTeamViewSettings()) }) {
    override suspend fun constructWidget(settings: ExternalTeamViewSettings): TeamViewWidget {
        val teamInfo = DataBus.contestInfoFlow.await().first().teams.find { it.id == settings.teamId }
        val content = settings.mediaTypes.mapNotNull { teamInfo?.medias?.get(it) }.toMutableList()
        if (settings.showTaskStatus) {
            settings.teamId?.let { teamId -> content.add(MediaType.TaskStatus(teamId)) }
        }
        if (settings.showAchievement) {
            teamInfo?.medias?.get(TeamMediaType.ACHIEVEMENT)?.let { content.add(it) }
        }
        return TeamViewWidget(OverlayTeamViewSettings(content, position))
    }

    override suspend fun createWidgetAndShow(settings: ExternalTeamViewSettings) {
        if (settings.teamId == null) {
            launchWhileWidgetShown {
                DataBus.teamSpotlightFlow.await().collect {
                    val staticSettings = settings.copy(teamId = it.teamId)
                    super.createWidgetAndShow(staticSettings)
                    delay(20.seconds)
                }
            }
        } else {
            super.createWidgetAndShow(settings)
        }
    }
}
