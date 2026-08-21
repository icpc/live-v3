package org.icpclive.controllers

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import org.icpclive.api.*
import org.icpclive.cds.api.TeamMediaType
import org.icpclive.data.*
import kotlin.time.Duration.Companion.seconds

class TeamViewController(manager: Manager<TeamViewWidget>, scope: CoroutineScope, val position: TeamViewPosition) :
    SingleWidgetController<ExternalTeamViewSettings, TeamViewWidget>(ExternalTeamViewSettings(), manager, scope) {

    override suspend fun constructWidgetFlow(settings: ExternalTeamViewSettings): Flow<TeamViewWidget> {
        val processedSettingsFlow = if (settings.teamId == null) {
            flow {
                DataBus.teamSpotlightFlow.await().collect { keyTeam ->
                    emit(settings.copy(teamId = keyTeam.teamId))
                    delay(30.seconds)
                }
            }
        } else {
            flowOf(settings)
        }
        return DataBus.currentContestInfoFlow().combine(processedSettingsFlow) { contestInfo, settings ->
            val teamInfo = contestInfo.teams[settings.teamId] ?: return@combine null
            val content = settings.mediaTypes.mapNotNull { teamInfo.medias[it] }.toList()

            val primary = content.getOrNull(0).orEmpty()
            val secondary = content.getOrNull(1).orEmpty()
            val achievement = teamInfo.medias[TeamMediaType.ACHIEVEMENT]?.takeIf { settings.showAchievement }.orEmpty()

            TeamViewWidget(
                OverlayTeamViewSettings(
                    teamInfo.id,
                    primary,
                    secondary,
                    settings.showTaskStatus,
                    achievement,
                    settings.showTimeLine,
                    position
                )
            )
        }.filterNotNull()
            .distinctUntilChangedBy { it.settings }
    }
}
