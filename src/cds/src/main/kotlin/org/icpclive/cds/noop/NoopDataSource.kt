package org.icpclive.cds.noop

import kotlinx.coroutines.flow.*
import kotlinx.datetime.Instant
import org.icpclive.api.*
import org.icpclive.cds.*
import org.icpclive.cds.common.ContestParseResult
import org.icpclive.cds.common.RawContestDataSource
import kotlin.time.Duration

internal class NoopDataSource : RawContestDataSource {

    override fun getFlow() = flowOf(
        InfoUpdate(ContestInfo(
            name = "",
            status = ContestStatus.BEFORE,
            startTime = Instant.DISTANT_FUTURE,
            contestLength = Duration.ZERO,
            freezeTime = Duration.ZERO,
            problems = emptyList(),
            teams = emptyList(),
            resultType = ContestResultType.ICPC,
            groups = emptyList(),
            penaltyRoundingMode = PenaltyRoundingMode.EACH_SUBMISSION_DOWN_TO_MINUTE
        ))
    )

    override suspend fun loadOnce() = ContestParseResult(
        getFlow().first().newInfo,
        emptyList(),
        emptyList()
    )
}