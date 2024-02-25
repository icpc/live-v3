package org.icpclive.clics.objects

import kotlinx.datetime.Instant
import org.icpclive.ksp.clics.*
import kotlin.time.Duration

@SinceClics(FeedVersion.`2020_03`)
@EventSerialName("clarifications")
public interface Clarification {
    @Required public val id: String
    public val fromTeamId: String?
    public val toTeamId: String?
    public val replyToId: String?
    public val problemId: String?
    public val text: String?
    public val time: Instant?
    public val contestTime: Duration?
}