package org.icpclive.cds.api

import kotlinx.datetime.Instant
import kotlinx.serialization.*
import org.icpclive.util.DurationInMillisecondsSerializer
import org.icpclive.util.UnixMillisecondsSerializer
import kotlin.time.Duration

@Serializable
public data class AnalyticsCompanionPreset(
    val presetId: Int,
    @SerialName("expirationTimeUnixMs")
    @Serializable(with = UnixMillisecondsSerializer::class)
    val expirationTime: Instant?,
)

@Serializable
public data class AnalyticsCompanionRun(
    @SerialName("expirationTimeUnixMs")
    @Serializable(with = UnixMillisecondsSerializer::class)
    val expirationTime: Instant?,
    val mediaType: MediaType,
)

@Serializable
public sealed class AnalyticsMessage {
    public abstract val id: String
    public abstract val time: Instant
    public abstract val relativeTime: Duration
}

@Serializable
@SerialName("commentary")
public data class AnalyticsCommentaryEvent(
    override val id: String,
    val message: String,
    @SerialName("timeUnixMs")
    @Serializable(with = UnixMillisecondsSerializer::class)
    override val time: Instant,
    @SerialName("relativeTimeMs")
    @Serializable(with = DurationInMillisecondsSerializer::class)
    override val relativeTime: Duration,
    val teamIds: List<TeamId>,
    val runIds: List<Int>,
    @Required val priority: Int = 0,
    @Required val tags: List<String> = emptyList(), // todo: support tage in CLICS parser
    @Required val advertisement: AnalyticsCompanionPreset? = null,
    @Required val tickerMessage: AnalyticsCompanionPreset? = null,
    @Required val featuredRun: AnalyticsCompanionRun? = null,
) : AnalyticsMessage()
