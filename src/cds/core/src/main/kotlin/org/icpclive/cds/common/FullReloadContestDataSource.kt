package org.icpclive.cds.common

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import org.icpclive.cds.api.ContestStatus
import org.icpclive.cds.*
import org.icpclive.util.getLogger
import org.icpclive.util.loopFlow
import kotlin.time.*

public abstract class FullReloadContestDataSource(private val interval: Duration) : ContestDataSource {
    public abstract suspend fun loadOnce(): ContestParseResult

    private var isOver = false

    override fun getFlow(): Flow<ContestUpdate> = loopFlow(
        interval,
        { getLogger(FullReloadContestDataSource::class).error("Failed to reload data, retrying", it) }
    ) {
        val loadStart = TimeSource.Monotonic.markNow()
        loadOnce().also {
            getLogger(FullReloadContestDataSource::class).info("Reloaded contest data in ${loadStart.elapsedNow()}")
        }
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