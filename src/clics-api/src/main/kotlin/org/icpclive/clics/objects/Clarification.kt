package org.icpclive.clics.objects

import kotlinx.datetime.Instant
import org.icpclive.ksp.clics.*
import kotlin.time.Duration

@SinceClics(FeedVersion.`2020_03`)
@EventSerialName("clarifications")
public data class Clarification(
    @Required public val id: String,
    public val fromTeamId: String? = null,
    public val toTeamId: String? = null,
    public val replyToId: String? = null,
    public val problemId: String? = null,
    public val text: String? = null,
    public val time: Instant? = null,
    public val contestTime: Duration? = null
)
