package org.icpclive.widget

import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.icpclive.api.TeamViewPosition
import org.icpclive.api.TeamViewSettings
import org.icpclive.api.TeamViewWidget
import org.icpclive.data.DataBus
import org.icpclive.data.Manager
import kotlin.time.Duration.Companion.seconds

class TeamViewController(val manager: Manager<TeamViewWidget>, val position: TeamViewPosition) :
    WidgetWrapper<TeamViewSettings, TeamViewWidget>(
        TeamViewSettings(),
        manager,
        { settings -> TeamViewWidget(settings, position) }),
    SingleWidgetController<TeamViewSettings, TeamViewWidget> {
    private var autoModeJob: Job? = null

    override suspend fun createWidgetAndShow(settings: TeamViewSettings) {
        if (settings.teamId == null) {
            autoModeJob = scope.launch {
                DataBus.teamSpotlightFlow.await().collect {
                    val staticSettings = settings.copy(teamId = it.teamId, position = position)
                    super.createWidgetAndShow(staticSettings)
                    delay(20.seconds)
                }
            }
        } else {
            super.createWidgetAndShow(settings.copy(position = position))
        }
    }
}
