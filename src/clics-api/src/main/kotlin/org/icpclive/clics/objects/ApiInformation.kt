package org.icpclive.clics.objects

import org.icpclive.ksp.clics.*

@SinceClics(FeedVersion.`2022_07`)
@NoEvent
public data class ApiInformation(
    public val version: String? = null,
    public val versionUrl: String? = null,
    @InlinedBefore(FeedVersion.`2023_06`, "") public val provider: ApiInformationProvider? = null
)
