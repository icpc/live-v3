package org.icpclive.cds

import org.icpclive.api.*

sealed interface ContestUpdate
data class InfoUpdate(val newInfo: ContestInfo) : ContestUpdate
data class RunUpdate(val newInfo: RunInfo) : ContestUpdate
data class AnalyticsUpdate(val message: AnalyticsMessage) : ContestUpdate

