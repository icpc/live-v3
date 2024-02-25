package org.icpclive.clics.objects

import org.icpclive.ksp.clics.*

@SinceClics(FeedVersion.`2023_06`)
@NoEvent
public interface ApiInformationProvider {
    public val name: String?
    @SinceClics(FeedVersion.`2023_06`) public val version: String?
    public val logo: List<File>
}