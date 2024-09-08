package org.icpclive.api

import kotlinx.datetime.Instant
import kotlinx.serialization.*
import org.icpclive.cds.api.*
import org.icpclive.cds.util.serializers.DurationInMillisecondsSerializer
import org.icpclive.cds.util.serializers.UnixMillisecondsSerializer
import kotlin.time.Duration

@JvmInline
@Serializable
value class AnalyticsMessageId internal constructor(val value: String) {
    override fun toString(): String = value
}

fun String.toAnalyticsMessageId() = AnalyticsMessageId(this)
fun RunId.toAnalyticsMessageId() = AnalyticsMessageId("run_$this")
fun CommentaryMessageId.toAnalyticsMessageId() = AnalyticsMessageId("commentary_$this")

@Serializable
data class AnalyticsMessage(
    val id: AnalyticsMessageId,
    @SerialName("updateTimeUnixMs")
    @Serializable(with = UnixMillisecondsSerializer::class)
    val lastUpdateTime: Instant,
    @SerialName("timeUnixMs")
    @Serializable(with = UnixMillisecondsSerializer::class)
    val time: Instant,
    @SerialName("relativeTimeMs")
    @Serializable(with = DurationInMillisecondsSerializer::class)
    val relativeTime: Duration,
    @Required val comments: List<AnalyticsMessageComment> = emptyList(),
    @Required val teamId: TeamId? = null,
    @Required val runInfo: RunInfo? = null,
    @Required val featuredRun: AnalyticsCompanionRun? = null,
    @Required val tags: Set<String> = emptySet(),
)


@Serializable
data class AnalyticsMessageComment(
    val id: CommentaryMessageId,
    val message: String,
    @Required val advertisement: AnalyticsCompanionPreset? = null,
    @Required val tickerMessage: AnalyticsCompanionPreset? = null,
    @SerialName("creationTimeUnixMs")
    @Serializable(with = UnixMillisecondsSerializer::class)
    val creationTime: Instant,
)


@Serializable
data class AnalyticsCompanionPreset(
    val presetId: Int,
    @SerialName("expirationTimeUnixMs")
    @Serializable(with = UnixMillisecondsSerializer::class)
    val expirationTime: Instant?,
)

@Serializable
data class AnalyticsCompanionRun(
    @SerialName("expirationTimeUnixMs")
    @Serializable(with = UnixMillisecondsSerializer::class)
    val expirationTime: Instant?,
    val mediaType: MediaType,
)
