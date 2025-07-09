package org.icpclive.clics.objects

import org.icpclive.ksp.clics.*

@SinceClics(FeedVersion.`2020_03`)
@EventSerialName("awards")
public data class Award(
    @Required public val id: String,
    public val citation: String? = null,
    public val teamIds: List<String> = emptyList()
)
