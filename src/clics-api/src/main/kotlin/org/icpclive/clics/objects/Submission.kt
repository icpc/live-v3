package org.icpclive.clics.objects

import org.icpclive.ksp.clics.*
import kotlin.time.Duration
import kotlin.time.Instant

@SinceClics(FeedVersion.`2020_03`)
@EventSerialName("submissions")
public data class Submission(
    @Required override val id: String,
    public val languageId: String? = null,
    @Required public val teamId: String,
    @Required public val problemId: String,
    public val time: Instant? = null,
    @Required public val contestTime: Duration,
    public val entryPoint: String? = null,
    public val files: List<File> = emptyList(),
    public val reaction: List<File>? = null
): ObjectWithId
