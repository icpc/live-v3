package org.icpclive.clics.objects

import org.icpclive.ksp.clics.*
import kotlin.time.Duration
import kotlin.time.Instant

@SinceClics(FeedVersion.`2020_03`)
@EventSerialName("clarifications")
public data class Clarification(
    @Required override val id: String,
    public val fromTeamId: String? = null,
    @SingleBefore(FeedVersion.DRAFT, "to_team_id") public val toTeamIds: List<String> = emptyList(),
    @SinceClics(FeedVersion.DRAFT) public val toGroupIds: List<String> = emptyList(),
    public val replyToId: String? = null,
    public val problemId: String? = null,
    public val text: String? = null,
    public val time: Instant? = null,
    public val contestTime: Duration? = null
): ObjectWithId
