package org.icpclive.clics.objects

import org.icpclive.ksp.clics.*

@SinceClics(FeedVersion.`2022_07`)
@NoEvent
public interface ApiInformation {
    public val version: String?
    public val versionUrl: String?
    @InlinedBefore(FeedVersion.`2023_06`, "") public val provider: ApiInformationProvider?
}

