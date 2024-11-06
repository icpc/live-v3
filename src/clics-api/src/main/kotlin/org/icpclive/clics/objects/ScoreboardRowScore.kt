package org.icpclive.clics.objects

import org.icpclive.ksp.clics.*
import kotlin.time.Duration

@SinceClics(FeedVersion.`2020_03`)
@NoEvent
public interface ScoreboardRowScore {
    @Required
    public val numSolved: Int
    @Required
    @LongMinutesBefore(FeedVersion.DRAFT)
    public val totalTime: Duration
}