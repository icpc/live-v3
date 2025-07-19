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
    @SinceClics(FeedVersion.DRAFT) public val memoryLimit: Double? = null,
    @SinceClics(FeedVersion.DRAFT) public val outputLimit: Double? = null,
    @SinceClics(FeedVersion.DRAFT) public val codeLimit: Double? = null,
    public val testDataCount: Int? = null,
    public val maxScore: Double? = null,
    @SinceClics(FeedVersion.`2022_07`) @JsonName("package") public val problemPackage: List<File>? = null,
    @SinceClics(FeedVersion.`2022_07`) public val statement: List<File>? = null,
)
