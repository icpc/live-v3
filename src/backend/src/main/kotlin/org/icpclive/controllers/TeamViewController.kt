package org.icpclive.controllers

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.filter
import org.icpclive.api.*
import org.icpclive.cds.api.TeamMediaType
import org.icpclive.data.*
import kotlin.time.Duration.Companion.seconds

class TeamViewController(manager: Manager<in TeamViewWidget>, val position: TeamViewPosition) :
    SingleWidgetController<ExternalTeamViewSettings, TeamViewWidget>(ExternalTeamViewSettings(), manager) {
    override suspend fun onDelete(id: Int) {}

    override suspend fun constructWidget(settings: ExternalTeamViewSettings): TeamViewWidget {
        val teamInfo = DataBus.currentContestInfo().teams[settings.teamId]
        val content = settings.mediaTypes.mapNotNull { teamInfo?.medias?.get(it) }.toList()

        val primary = content.getOrNull(0).orEmpty()
        val secondary = content.getOrNull(1).orEmpty()
        val achievement = teamInfo?.medias?.get(TeamMediaType.ACHIEVEMENT)?.takeIf { settings.showAchievement }.orEmpty()

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
                DataBus.teamSpotlightFlow.await()
                    .filter {
                        val teamInfo = DataBus.currentContestInfo().teams[it.teamId]
                        return@filter teamInfo?.isHidden?.not() ?: false
                    }
                    .collect {
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
