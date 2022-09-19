package org.icpclive.widget

import kotlinx.coroutines.delay
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
        { settings -> TeamViewWidget(settings) }),
    SingleWidgetController<TeamViewSettings, TeamViewWidget> {
    override suspend fun createWidgetAndShow(settings: TeamViewSettings) {
        if (settings.teamId == null) {
            launchWhileWidgetShown {
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
