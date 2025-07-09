package org.icpclive.clics.objects

import org.icpclive.ksp.clics.*

@SinceClics(FeedVersion.`2023_06`)
@NoEvent
public data class ApiInformationProvider(
    public val name: String? = null,
    @SinceClics(FeedVersion.`2023_06`) public val version: String? = null,
    public val logo: List<File> = emptyList()
)
