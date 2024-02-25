package org.icpclive.clics.objects

import org.icpclive.ksp.clics.*

@SinceClics(FeedVersion.`2020_03`)
@NoEvent
public interface ScoreboardRowScore {
    @Required
    public val numSolved: Int
    @Required
    public val totalTime: Long
}