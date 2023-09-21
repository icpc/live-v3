package org.icpclive.cds.noop

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.datetime.Instant
import org.icpclive.api.*
import org.icpclive.cds.InfoUpdate
import org.icpclive.cds.common.*
import org.icpclive.cds.common.ContestDataSource
import kotlin.time.Duration

internal class NoopDataSource : ContestDataSource {

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
}