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
        val content = mutableListOf<MediaType>()
        settings.mediaTypes.forEach {
            val url = teamInfo?.medias?.get(it) ?: return@forEach
            content += when (it) {
                TeamMediaType.CAMERA -> MediaType.WebRTCConnection(url)
                TeamMediaType.SCREEN -> MediaType.WebRTCConnection(url)
                TeamMediaType.REACTION_VIDEO -> MediaType.Video(url)
                TeamMediaType.RECORD -> MediaType.Video(url)
                TeamMediaType.PHOTO -> MediaType.Photo(url)
                else -> null
            } ?: return@forEach
        }
        if (settings.showTaskStatus) {
            settings.teamId?.let { teamId -> content.add(MediaType.TaskStatus(teamId)) }
        }
        if (settings.showAchievement) {
            teamInfo?.medias?.get(TeamMediaType.ACHIEVEMENT)?.let { content.add(MediaType.TeamAchievements(it)) }
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
