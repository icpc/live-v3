package org.icpclive.clics.objects

import org.icpclive.ksp.clics.*

@SinceClics(FeedVersion.`2020_03`)
@UpdateContestEvent
@EventSerialName("groups")
public interface Group {
    @Required public val id: String
    public val icpcId: String?
    public val name: String?
    public val type: String?
    public val location: Location?
}