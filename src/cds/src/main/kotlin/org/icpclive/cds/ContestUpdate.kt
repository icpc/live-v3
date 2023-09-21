package org.icpclive.cds

import org.icpclive.api.*

public sealed interface ContestUpdate
public data class InfoUpdate(val newInfo: ContestInfo) : ContestUpdate
public data class RunUpdate(val newInfo: RunInfo) : ContestUpdate
public data class AnalyticsUpdate(val message: AnalyticsMessage) : ContestUpdate

