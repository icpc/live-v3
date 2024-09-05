package org.icpclive.cds.api

import kotlinx.datetime.Instant
import kotlinx.serialization.*
import org.icpclive.cds.util.serializers.DurationInMillisecondsSerializer
import org.icpclive.cds.util.serializers.UnixMillisecondsSerializer
import kotlin.time.Duration

@Serializable
public data class CommentaryMessage(
    val id: String,
    val message: String,
    @SerialName("timeUnixMs")
    @Serializable(with = UnixMillisecondsSerializer::class)
    val time: Instant,
    @SerialName("relativeTimeMs")
    @Serializable(with = DurationInMillisecondsSerializer::class)
    val relativeTime: Duration,
    val teamIds: List<TeamId>,
    val runIds: List<RunId>,
    @Required val priority: Int = 0,
    @Required val tags: List<String> = emptyList(), // todo: support tage in CLICS parser
)
