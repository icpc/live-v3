package org.icpclive.api

import kotlinx.serialization.*
import org.icpclive.cds.api.TeamId
import org.icpclive.cds.util.serializers.DurationInSecondsSerializer
import kotlin.time.Duration
import kotlin.time.TimeSource

@Serializable
data class WidgetUsageStatistics(
    val entries: MutableMap<String, WidgetUsageStatisticsEntry>
)

@Serializable
sealed class WidgetUsageStatisticsEntry {
    @Serializable
    @SerialName("simple")
    data class Simple(
        @Transient val shownSince: TimeSource.Monotonic.ValueTimeMark? = null,
        @Transient val shownCount: Int = 0,
        @Serializable(with = DurationInSecondsSerializer::class)
        @SerialName("totalShownTimeSeconds")
        val totalShownTime: Duration
    ) : WidgetUsageStatisticsEntry()
    @Serializable
    @SerialName("per_team")
    data class PerTeam(val byTeam: Map<TeamId, WidgetUsageStatisticsEntry>): WidgetUsageStatisticsEntry()
}