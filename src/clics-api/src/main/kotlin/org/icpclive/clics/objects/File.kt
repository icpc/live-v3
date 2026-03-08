package org.icpclive.clics.objects

import org.icpclive.clics.Url
import org.icpclive.ksp.clics.*

@SinceClics(FeedVersion.`2020_03`)
@NoEvent
public data class File(
    public val mime: String? = null,
    public val href: Url? = null,
    public val width: Int? = null,
    public val height: Int? = null,
    @SinceClics(FeedVersion.`2022_07`) public val filename: String? = null,
    @SinceClics(FeedVersion.`2022_07`) public val hash: String? = null,
    @SinceClics(FeedVersion.`2026_01`) public val tag: List<String> = emptyList(),
)
