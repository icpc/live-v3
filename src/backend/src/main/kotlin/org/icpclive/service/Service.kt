package org.icpclive.service

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import org.icpclive.cds.scoreboard.ContestStateWithScoreboard

fun interface Service {
    fun CoroutineScope.runOn(flow: Flow<ContestStateWithScoreboard>)
}