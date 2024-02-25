package org.icpclive.clics.objects

import org.icpclive.ksp.clics.*

@SinceClics(FeedVersion.`2020_03`)
@UpdateContestEvent
@EventSerialName("awards")
public interface Award {
    @Required public val id: String
    public val citation: String?
    public val teamIds: List<String>
}