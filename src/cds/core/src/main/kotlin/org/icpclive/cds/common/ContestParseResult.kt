package org.icpclive.cds.common

import kotlinx.serialization.Serializable
import org.icpclive.cds.api.*

@Serializable
public data class ContestParseResult(
    val contestInfo: ContestInfo,
    val runs: List<RunInfo>,
    val analyticsMessages: List<AnalyticsMessage>,
)