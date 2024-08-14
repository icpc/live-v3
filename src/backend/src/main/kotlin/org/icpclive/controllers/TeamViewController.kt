package org.icpclive.controllers

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import org.icpclive.api.*
import org.icpclive.cds.api.MediaType
import org.icpclive.cds.api.TeamMediaType
import org.icpclive.data.*
import kotlin.time.Duration.Companion.seconds

class TeamViewController(manager: Manager<TeamViewWidget>, val position: TeamViewPosition) :
    SingleWidgetController<ExternalTeamViewSettings, TeamViewWidget>(ExternalTeamViewSettings(), manager) {
    override suspend fun onDelete(id: Int) {}

    override suspend fun constructWidget(settings: ExternalTeamViewSettings): TeamViewWidget {
        val teamInfo = DataBus.currentContestInfo().teams[settings.teamId]
        val content = settings.mediaTypes.mapNotNull { teamInfo?.medias?.get(it) }.toList()

        val primary = content.getOrNull(0)
        val secondary = content.getOrNull(1)
        val achievement = teamInfo?.medias?.get(TeamMediaType.ACHIEVEMENT).takeIf { settings.showAchievement }

        return TeamViewWidget(
            OverlayTeamViewSettings(
                teamInfo?.id!!,
                primary,
                secondary,
                settings.showTaskStatus,
                achievement,
                settings.showTimeLine,
                position
            )
        )
    }

    override suspend fun createWidgetAndShow(settings: ExternalTeamViewSettings) {
        if (settings.teamId == null) {
            launchWhileWidgetShown {
                DataBus.teamSpotlightFlow.await().collect {
                    val staticSettings = settings.copy(teamId = it.teamId)
                    super.createWidgetAndShow(staticSettings)
                    delay(30.seconds)
                }
            }
        } else {
            super.createWidgetAndShow(settings)
        }
    }
}
