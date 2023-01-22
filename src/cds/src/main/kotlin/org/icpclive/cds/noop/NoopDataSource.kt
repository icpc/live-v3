package org.icpclive.cds.noop

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.datetime.Instant
import org.icpclive.api.*
import org.icpclive.cds.ContestParseResult
import org.icpclive.cds.RawContestDataSource
import kotlin.time.Duration

class NoopDataSource : RawContestDataSource {
    val noopContestInfo = ContestInfo(
        status = ContestStatus.BEFORE,
        startTime = Instant.DISTANT_FUTURE,
        contestLength = Duration.ZERO,
        freezeTime = Duration.ZERO,
        problems = emptyList(),
        teams = emptyList(),
        resultType = ContestResultType.ICPC,
    )

    override suspend fun loadOnce() = ContestParseResult(
        noopContestInfo,
        emptyList(),
        emptyList()
    )

    override suspend fun run(
        contestInfoDeferred: CompletableDeferred<StateFlow<ContestInfo>>,
        runsDeferred: CompletableDeferred<Flow<RunInfo>>,
        analyticsMessagesDeferred: CompletableDeferred<Flow<AnalyticsMessage>>
    ) {
        contestInfoDeferred.complete(MutableStateFlow(noopContestInfo))
        runsDeferred.complete(emptyFlow())
        analyticsMessagesDeferred.complete(emptyFlow())
    }
}