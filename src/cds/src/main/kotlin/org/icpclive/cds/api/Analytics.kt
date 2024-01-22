package org.icpclive.cds.api

import kotlinx.datetime.Instant
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
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
    val teamIds: List<Int>,
    val runIds: List<Int>,
    val priority: Int = 0,
    val tags: List<String> = emptyList(), // todo: support tage in CLICS parser
    val advertisement: AnalyticsCompanionPreset? = null,
    val tickerMessage: AnalyticsCompanionPreset? = null,
    val featuredRun: AnalyticsCompanionRun? = null,
) : AnalyticsMessage()
