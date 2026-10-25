package org.icpclive.controllers

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.*
import org.icpclive.api.*
import org.icpclive.cds.api.ContestInfo
import org.icpclive.data.Manager

class LocatorWidgetController(
    manager: Manager<TeamLocatorWidget>,
    parentScope: CoroutineScope,
    showOrderCounter: ShowOrderCounter,
    private val contestInfoFlow: Flow<ContestInfo>,
) : SingleWidgetController<ExternalTeamLocatorSettings, TeamLocatorWidget>(
    ExternalTeamLocatorSettings(), manager, parentScope, showOrderCounter
) {

    override suspend fun constructWidgetFlow(settings: ExternalTeamLocatorSettings): Flow<TeamLocatorWidget> {
        return contestInfoFlow.map { info ->
            TeamLocatorWidget(
                TeamLocatorSettings(
                    settings.circles
                        .filter { it.teamId in info.teams }
                        .map { TeamLocatorCircleSettings(it.x, it.y, it.radius, it.teamId!!) },
                    settings.scene
                )
            )
        }.distinctUntilChangedBy { it.settings }
    }
}