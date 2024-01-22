package org.icpclive.cds.common

import kotlinx.serialization.Serializable
import org.icpclive.cds.api.*

@Serializable
internal data class ContestParseResult(
    val contestInfo: ContestInfo,
    val runs: List<RunInfo>,
    val analyticsMessages: List<AnalyticsMessage>,
)