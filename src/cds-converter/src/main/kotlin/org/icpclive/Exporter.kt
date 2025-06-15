package org.icpclive

import io.ktor.server.routing.Route
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import org.icpclive.cds.scoreboard.ContestStateWithScoreboard

interface Exporter {
    fun Route.setUp(scope: CoroutineScope, contestUpdates: Flow<ContestStateWithScoreboard>)
}