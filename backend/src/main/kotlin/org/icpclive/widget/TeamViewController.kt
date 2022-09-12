package org.icpclive.widget

import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.icpclive.api.*
import org.icpclive.data.DataBus
import org.icpclive.data.Manager
import kotlin.time.Duration.Companion.seconds

class TeamViewController(val manager: Manager<TeamViewWidget>) :
    AbstractWidgetWrapper<TeamViewSettings, TeamViewWidget>(TeamViewSettings()),
    SingleWidgetController<TeamViewSettings, TeamViewWidget> {
    private var overlayWidgetId: String? = null
    private var autoModeJob: Job? = null

    private suspend fun createWidgetAndShow(settings: TeamViewInstanceSettings) {
        val widget = TeamViewWidget(settings)
        manager.add(widget)
        overlayWidgetId = widget.id
    }

    override suspend fun createWidgetAndShow(settings: TeamViewSettings) {
        if (settings.autoMode) {
            autoModeJob = scope.launch {
                DataBus.teamSpotlightFlow.await().collect {
                    val staticSettings = TeamViewInstanceSettings(
                        it.teamId, when (it.cause) {
                            is RunCause -> MediaType.CAMERA
                            is ScoreSumCause -> MediaType.SCREEN
                        }
                    )
                    createWidgetAndShow(staticSettings)

                    delay(20.seconds)
                }
            }
        } else {
            settings.instance?.let { createWidgetAndShow(it) }
        }
    }

    override suspend fun removeWidget() {
        overlayWidgetId?.let { manager.remove(it) }
        overlayWidgetId = null
    }

    override suspend fun getStatus(): ObjectStatus<TeamViewSettings> {
        return ObjectStatus(overlayWidgetId != null, settings)
    }
}
