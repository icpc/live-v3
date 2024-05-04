package org.icpclive.cds

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import org.icpclive.cds.util.*
import org.icpclive.cds.api.ContestStatus
import org.icpclive.cds.util.loopFlow
import kotlin.time.*

public abstract class FullReloadContestDataSource(private val interval: Duration) : ContestDataSource {
    public abstract suspend fun loadOnce(): ContestParseResult

    private var isOver = false

    override fun getFlow(): Flow<ContestUpdate> = loopFlow(
        interval,
        { logger.error(it) { "Failed to reload data, retrying" } }
    ) {
        val loadStart = TimeSource.Monotonic.markNow()
        loadOnce().also {
            logger.info { "Reloaded contest data in ${loadStart.elapsedNow()}" }
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

    private companion object {
        val logger by getLogger()
    }
}