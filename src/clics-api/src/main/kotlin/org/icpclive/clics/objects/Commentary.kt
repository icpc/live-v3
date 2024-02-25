package org.icpclive.clics.objects

import kotlinx.datetime.Instant
import org.icpclive.ksp.clics.*
import kotlin.time.Duration

@SinceClics(FeedVersion.`2020_03`)
@EventSerialName("commentary")
public interface Commentary {
    @Required public val id: String
    @Required public val time: Instant
    @Required public val contestTime: Duration
    @Required public val message: String
    @SinceClics(FeedVersion.`2022_07`) public val tags: List<String>
    @SinceClics(FeedVersion.`2022_07`) public val sourceId: String?
    public val teamIds: List<String>?
    public val problemIds: List<String>?
    public val submissionIds: List<String>?
}