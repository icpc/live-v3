package org.icpclive.cds.api

import kotlinx.serialization.*
import org.icpclive.cds.util.serializers.DurationInMillisecondsSerializer
import org.icpclive.cds.util.serializers.UnixMillisecondsSerializer
import kotlin.time.Duration
import kotlin.time.Instant

@JvmInline
@Serializable
public value class CommentaryMessageId internal constructor(public val value: String) {
    override fun toString(): String = value
}

public fun String.toCommentaryMessageId(): CommentaryMessageId = CommentaryMessageId(this)

@Serializable
public data class CommentaryMessage(
    val id: CommentaryMessageId,
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
