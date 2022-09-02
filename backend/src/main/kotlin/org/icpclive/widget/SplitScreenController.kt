package org.icpclive.widget

import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import org.icpclive.api.*
import org.icpclive.data.DataBus
import org.icpclive.data.Manager

class SplitScreenController(val manager: Manager<TeamViewWidget>) :
    AbstractWidgetWrapper<SplitScreenSettings, TeamViewWidget>(SplitScreenSettings(true, emptyMap())),
    SingleWidgetController<SplitScreenSettings, TeamViewWidget> {
    private var isShown = false
    private val overlayWidgetIds = mutableMapOf<TeamViewPosition, String>()
    private var autoModeJob: Job? = null

    private fun widgetConstructor(settings: TeamViewSettings, position: TeamViewPosition) =
        TeamViewWidget(settings, position)

    private suspend fun createWidgetInstanceAndShow(settings: TeamViewSettings, position: TeamViewPosition) {
        val widget = widgetConstructor(settings, position)
        manager.add(widget)
        overlayWidgetIds[position] = widget.id
    }

    override suspend fun createWidgetAndShow() {
        isShown = true
        if (settings.autoMode) {
            autoModeJob = scope.launch {
                val positions = TeamViewPosition.values()
                var nextInstanceNumber = 0
                DataBus.autoSplitScreenTeams.await().collect {
                    val position = positions[nextInstanceNumber++ % positions.size]
                    createWidgetInstanceAndShow(TeamViewSettings(it.teamId, when (it.cause) {
                        is RunCause -> MediaType.CAMERA
                        is ScoreSumCause -> MediaType.SCREEN
                    }), position)
                }
            }
            return
        }
        for ((position, instanceSetting) in settings.instances) {
            createWidgetInstanceAndShow(instanceSetting, position)
        }
    }

    override suspend fun removeWidget() {
        autoModeJob?.cancel()
        overlayWidgetIds.values.forEach { manager.remove(it) }
        overlayWidgetIds.clear()
        isShown = false
    }

    override suspend fun getStatus() = ObjectStatus(isShown, settings)
}

