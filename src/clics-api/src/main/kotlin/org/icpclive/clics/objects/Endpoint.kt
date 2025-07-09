package org.icpclive.clics.objects

import org.icpclive.ksp.clics.*

@SinceClics(FeedVersion.`2022_07`)
@NoEvent
public data class Endpoint(
    public val type: String? = null,
    public val properties: List<String> = emptyList()
)
