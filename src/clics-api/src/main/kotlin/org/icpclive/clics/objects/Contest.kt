package org.icpclive.clics.objects

import kotlinx.datetime.Instant
import org.icpclive.ksp.clics.*
import kotlin.time.*

@SinceClics(FeedVersion.`2020_03`)
@UpdateContestEvent
@EventSerialName("contests", "contest")
public interface Contest {
    @Required public val id: String
    public val name: String?
    public val formalName: String?
    public val startTime: Instant?
    public val countdownPauseTime: Duration?
    @Required public val duration: Duration
    public val scoreboardFreezeDuration: Duration?
    @SinceClics(FeedVersion.`2023_06`) public val scoreboardThawTime: Instant?
    public val scoreboardType: String?
    public val penaltyTime: Long?
    public val banner: List<File>
    public val logo: List<File>
    @InlinedBefore(FeedVersion.`2023_06`, "location.") public val location: Location?
}