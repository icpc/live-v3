package org.icpclive.clics.objects

import org.icpclive.ksp.clics.*
import kotlin.time.Duration
import kotlin.time.Instant

@SinceClics(FeedVersion.`2020_03`)
@EventSerialName("commentary")
public data class Commentary(
    @Required override val id: String,
    @Required public val time: Instant,
    @Required public val contestTime: Duration,
    @Required public val message: String,
    @SinceClics(FeedVersion.`2022_07`) public val tags: List<String> = emptyList(),
    @SinceClics(FeedVersion.`2022_07`) public val sourceId: String? = null,
    public val teamIds: List<String>? = null,
    public val problemIds: List<String>? = null,
    public val submissionIds: List<String>? = null
): ObjectWithId
