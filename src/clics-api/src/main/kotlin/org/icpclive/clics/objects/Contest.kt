package org.icpclive.clics.objects

import org.icpclive.ksp.clics.*
import kotlin.time.Duration
import kotlin.time.Instant

@SinceClics(FeedVersion.`2020_03`)
@EventSerialName("contests", "contest")
public data class Contest(
    @Required public val id: String,
    public val name: String? = null,
    public val formalName: String? = null,
    public val startTime: Instant? = null,
    public val countdownPauseTime: Duration? = null,
    @Required public val duration: Duration,
    public val scoreboardFreezeDuration: Duration? = null,
    @SinceClics(FeedVersion.`2023_06`) public val scoreboardThawTime: Instant? = null,
    public val scoreboardType: String? = null,
    @LongMinutesBefore(FeedVersion.DRAFT) public val penaltyTime: Duration? = null,
    public val banner: List<File> = emptyList(),
    public val logo: List<File> = emptyList(),
    @InlinedBefore(FeedVersion.`2023_06`, "location.") public val location: Location? = null
)
