package org.icpclive.widget

import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.icpclive.api.*
import org.icpclive.data.DataBus
import org.icpclive.data.Manager
import kotlin.time.Duration.Companion.seconds

class SplitScreenController(val manager: Manager<TeamViewWidget>) :
    AbstractWidgetWrapper<SplitScreenSettings, TeamViewWidget>(SplitScreenSettings(true, emptyMap())) {
    private var isShown = false
    private val overlayWidgetIds = mutableMapOf<TeamViewPosition, String>()

    private fun widgetConstructor(settings: TeamViewSettings, position: TeamViewPosition) =
        TeamViewWidget(settings.copy(position=position))

    private suspend fun createWidgetInstanceAndShow(settings: TeamViewSettings, position: TeamViewPosition) {
        val widget = widgetConstructor(settings, position)
        manager.add(widget)
        overlayWidgetIds[position] = widget.id
    }

    override suspend fun createWidgetAndShow(settings: SplitScreenSettings) {
        isShown = true
        if (settings.autoMode) {
            launchWhileWidgetExists {
                val positions = TeamViewPosition.values()
                val currentTeams = MutableList(positions.size) { 0 }
                var setupInstanceNumber = 0
                var nextInstanceNumber = 0
                DataBus.teamSpotlightFlow.await().collect {
                    if (it.teamId in currentTeams) return@collect
                    currentTeams[nextInstanceNumber % positions.size] = it.teamId
                    val position = positions[nextInstanceNumber++ % positions.size]
                    val instanceSetting = TeamViewSettings(
                        it.teamId, when (it.cause) {
                            is RunCause -> MediaType.CAMERA
                            is ScoreSumCause -> MediaType.SCREEN
                        }
                    )
                    createWidgetInstanceAndShow(instanceSetting, position)
                    if (setupInstanceNumber < 4) {
                        setupInstanceNumber++
                    } else {
                        delay(5.seconds)
                    }
                }
            }
            return
        }
        for ((position, instanceSetting) in settings.instances) {
            createWidgetInstanceAndShow(instanceSetting, position)
        }
    }

    override suspend fun removeWidget() {
        overlayWidgetIds.values.forEach { manager.remove(it) }
        overlayWidgetIds.clear()
        isShown = false
    }

    override suspend fun getStatus() = ObjectStatus(isShown, settings)
}

