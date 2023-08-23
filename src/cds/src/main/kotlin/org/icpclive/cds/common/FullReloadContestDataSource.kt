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

    open val autoFinalize = true
    private fun ContestParseResult.isAutoFinal() =
        contestInfo.status == ContestStatus.OVER && runs.all { it.result != null }

    override fun getFlow() = loopFlow(
        interval,
        { getLogger(ContestDataSource::class).error("Failed to reload data, retrying", it) }
    ) {
        loadOnce()
    }.flowOn(Dispatchers.IO)
        .conflate()
        .transformWhile {
            val isFinal = it.contestInfo.status == ContestStatus.FINALIZED || (autoFinalize && it.isAutoFinal())
            if (isFinal) {
                emit(InfoUpdate(it.contestInfo.copy(status = ContestStatus.OVER)))
            } else {
                emit(InfoUpdate(it.contestInfo))
            }
            it.runs.forEach { run -> emit(RunUpdate(run)) }
            it.analyticsMessages.forEach { msg -> emit(AnalyticsUpdate(msg)) }
            if (isFinal) {
                emit(InfoUpdate(it.contestInfo.copy(status = ContestStatus.FINALIZED)))
                false
            } else {
                true
            }
        }
}