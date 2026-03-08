package org.icpclive.clics.objects

import org.icpclive.ksp.clics.*
import kotlin.time.Duration

@SinceClics(FeedVersion.`2020_03`)
@NoEvent
public data class ScoreboardRowScore(
    public val numSolved: Int? = null,
    @LongMinutesBefore(FeedVersion.`2026_01`)
    public val totalTime: Duration? = null,
    @SinceClics(FeedVersion.`2022_07`) public val score: Double? = null,
    @LongMinutesBefore(FeedVersion.`2026_01`)
    @SinceClics(FeedVersion.`2022_07`) public val time: Duration? = null,
)
