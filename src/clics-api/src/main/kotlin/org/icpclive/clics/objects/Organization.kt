package org.icpclive.clics.objects

import org.icpclive.ksp.clics.*

@SinceClics(FeedVersion.`2020_03`)
@UpdateContestEvent
@EventSerialName("organizations")
public interface Organization {
    @Required public val id: String
    public val icpcId: String?
    public val name: String?
    public val formalName: String?
    public val country: String?
    public val countryFlag: List<File>?
    public val url: String?
    public val twitterHashtag: String?
    @SinceClics(FeedVersion.`2023_06`) public val twitterAccount: String?
    public val location: Location?
    public val logo: List<File>?
}