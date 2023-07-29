package org.icpclive.cds

import org.icpclive.api.AnalyticsMessage
import org.icpclive.api.ContestInfo
import org.icpclive.api.RunInfo

sealed interface ContestUpdate
data class InfoUpdate(val newInfo: ContestInfo) : ContestUpdate
data class RunUpdate(val newInfo: RunInfo) : ContestUpdate
data class Analytics(val message: AnalyticsMessage) : ContestUpdate

