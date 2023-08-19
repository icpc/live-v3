package org.icpclive.cds.noop

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.datetime.Instant
import org.icpclive.api.*
import org.icpclive.cds.InfoUpdate
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
            problemList = emptyList(),
            teamList = emptyList(),
            resultType = ContestResultType.ICPC,
            groupList = emptyList(),
            organizationList = emptyList(),
            penaltyRoundingMode = PenaltyRoundingMode.EACH_SUBMISSION_DOWN_TO_MINUTE
        ))
    )

    override suspend fun loadOnce() = ContestParseResult(
        getFlow().first().newInfo,
        emptyList(),
        emptyList()
    )
}