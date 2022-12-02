package org.icpclive.api

import kotlinx.datetime.Instant
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.icpclive.util.DurationInMillisecondsSerializer
import org.icpclive.util.UnixMillisecondsSerializer
import kotlin.time.Duration

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

@Serializable
sealed class AnalyticsMessage {
    abstract val id: String
    abstract val time: Instant
    abstract val relativeTime: Duration
}

@Serializable
@SerialName("commentary")
data class AnalyticsCommentaryEvent(
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
