package org.icpclive.clics.objects

import org.icpclive.ksp.clics.*

@SinceClics(FeedVersion.`2020_03`)
@EventSerialName("languages")
public data class Language(
    @Required override val id: String,
    @Required public val name: String,
    @SinceClics(FeedVersion.`2022_07`) public val extensions: List<String> = emptyList()
): ObjectWithId
