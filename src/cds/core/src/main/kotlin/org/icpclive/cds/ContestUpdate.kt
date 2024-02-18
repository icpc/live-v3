package org.icpclive.cds

import org.icpclive.cds.api.*

public sealed interface ContestUpdate
public class InfoUpdate(public val newInfo: ContestInfo) : ContestUpdate {
    override fun toString(): String {
        return "InfoUpdate(newInfo=$newInfo)"
    }
}

public class RunUpdate(public val newInfo: RunInfo) : ContestUpdate {
    override fun toString(): String {
        return "RunUpdate(newInfo=$newInfo)"
    }
}

public class AnalyticsUpdate(public val message: AnalyticsMessage) : ContestUpdate {
    override fun toString(): String {
        return "AnalyticsUpdate(message=$message)"
    }
}

