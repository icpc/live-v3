package org.icpclive.clics.objects

import org.icpclive.clics.Url
import org.icpclive.ksp.clics.*

@SinceClics(FeedVersion.`2020_03`)
@NoEvent
public interface File {
    public val mime: String?
    public val href: Url?
    public val width: Int?
    public val height: Int?
    @SinceClics(FeedVersion.`2022_07`) public val fileName: String?
    @SinceClics(FeedVersion.`2022_07`) public val hash: String?
}