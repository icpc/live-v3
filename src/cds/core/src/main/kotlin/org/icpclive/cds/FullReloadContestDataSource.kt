package org.icpclive.cds

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import org.icpclive.cds.api.ContestStatus
import org.icpclive.cds.util.getLogger
import org.icpclive.cds.util.loopFlow
import kotlin.time.Duration
import kotlin.time.TimeSource

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
            val status = it.contestInfo.status
            if (!isOver && status is ContestStatus.OVER) {
                emit(InfoUpdate(it.contestInfo.copy(status = ContestStatus.RUNNING(status.startedAt, status.frozenAt, isFake = true))))
            } else {
                emit(InfoUpdate(it.contestInfo))
            }
            it.runs.sortedBy { it.time }.forEach { run -> emit(RunUpdate(run)) }
            it.commentaryMessages.forEach { msg -> emit(CommentaryMessagesUpdate(msg)) }
            if (!isOver && status is ContestStatus.OVER) {
                isOver = true
                emit(InfoUpdate(it.contestInfo))
            }
        }

    private companion object {
        val logger by getLogger()
    }
}
