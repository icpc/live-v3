package org.icpclive.cds.common

import kotlinx.serialization.Serializable
import org.icpclive.api.AnalyticsMessage
import org.icpclive.api.ContestInfo
import org.icpclive.api.RunInfo

@Serializable
data class ContestParseResult(
    val contestInfo: ContestInfo,
    val runs: List<RunInfo>,
    val analyticsMessages: List<AnalyticsMessage>
)