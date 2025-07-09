package org.icpclive.clics.objects

import org.icpclive.ksp.clics.*

@SinceClics(FeedVersion.`2020_03`)
@EventSerialName("problems")
public data class Problem(
    @Required public val id: String,
    public val uuid: String? = null,
    @Required public val ordinal: Int,
    @Required public val label: String,
    @Required public val name: String,
    public val rgb: String? = null,
    public val color: String? = null,
    public val timeLimit: Double? = null,
    public val testDataCount: Int? = null,
    public val maxScore: Double? = null,
    public val statement: List<File>? = null
)
