package org.icpclive.cds.common

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import org.icpclive.cds.Analytics
import org.icpclive.cds.InfoUpdate
import org.icpclive.cds.RunUpdate
import org.icpclive.util.getLogger
import org.icpclive.util.loopFlow
import kotlin.time.Duration

abstract class FullReloadContestDataSource(val interval: Duration) : RawContestDataSource {
    override fun getFlow() = flow {
        loopFlow(
            interval,
            { getLogger(ContestDataSource::class).error("Failed to reload data, retrying", it) }
        ) {
            loadOnce()
        }.flowOn(Dispatchers.IO)
            .collect {
                emit(InfoUpdate(it.contestInfo))
                it.runs.forEach { emit(RunUpdate(it)) }
                it.analyticsMessages.forEach { emit(Analytics(it)) }
            }
    }
}