package org.icpclive.clics.objects

import org.icpclive.ksp.clics.*

@SinceClics(FeedVersion.`2020_03`)
@EventSerialName("judgement-types")
public data class JudgementType(
    @Required public val id: String,
    @Required public val name: String,
    @Required public val solved: Boolean,
    @Required public val penalty: Boolean,
    @SinceClics(FeedVersion.DRAFT) public val simplifiedJudgementTypeId: String? = null,
)
