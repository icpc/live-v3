package org.icpclive.clics.objects

import org.icpclive.ksp.clics.*

@SinceClics(FeedVersion.`2020_03`)
@UpdateContestEvent
@EventSerialName("languages")
public interface Language {
    @Required public val id: String
}