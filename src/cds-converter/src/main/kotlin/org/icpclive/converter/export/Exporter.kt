package org.icpclive.converter.export

import io.ktor.server.routing.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import org.icpclive.cds.scoreboard.ContestStateWithScoreboard

interface Exporter {
    val subscriptionCount: Int
        get() = 1
    fun CoroutineScope.runOn(contestUpdates: Flow<ContestStateWithScoreboard>): Router
}

fun interface Router {
    fun Route.setUpRoutes()
}