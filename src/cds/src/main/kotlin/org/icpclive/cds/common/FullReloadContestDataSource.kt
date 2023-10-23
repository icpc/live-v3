package org.icpclive.cds.common

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import org.icpclive.api.ContestStatus
import org.icpclive.cds.*
import org.icpclive.util.getLogger
import org.icpclive.util.loopFlow
import kotlin.time.Duration

internal abstract class FullReloadContestDataSource(val interval: Duration) : ContestDataSource {
    abstract suspend fun loadOnce(): ContestParseResult

    var isOver = false

    override fun getFlow() = loopFlow(
        interval,
        { getLogger(FullReloadContestDataSource::class).error("Failed to reload data, retrying", it) }
    ) {
        loadOnce()
    }.flowOn(Dispatchers.IO)
        .conflate()
        .transform {
            if (!isOver && it.contestInfo.status == ContestStatus.OVER) {
                emit(InfoUpdate(it.contestInfo.copy(status = ContestStatus.FAKE_RUNNING)))
            } else {
                emit(InfoUpdate(it.contestInfo))
            }
            it.runs.sortedBy { it.time }.forEach { run -> emit(RunUpdate(run)) }
            it.analyticsMessages.forEach { msg -> emit(AnalyticsUpdate(msg)) }
            if (!isOver && it.contestInfo.status == ContestStatus.OVER) {
                isOver = true
                emit(InfoUpdate(it.contestInfo))
            }
        }
}