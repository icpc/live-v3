package org.icpclive.cds.plugins.noop

import kotlinx.coroutines.flow.flowOf
import kotlinx.datetime.Instant
import org.icpclive.cds.InfoUpdate
import org.icpclive.cds.api.*
import org.icpclive.cds.common.ContestDataSource
import org.icpclive.cds.ksp.Builder
import org.icpclive.cds.settings.CDSSettings
import kotlin.time.Duration


@Builder("noop")
public sealed interface NoopSettings : CDSSettings {
    override fun toDataSource(): ContestDataSource = NoopDataSource()
}

internal class NoopDataSource : ContestDataSource {

    override fun getFlow() = flowOf(
        InfoUpdate(
            ContestInfo(
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
            )
        )
    )
}