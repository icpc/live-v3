package org.icpclive.cds

import org.icpclive.api.MediaType
import org.icpclive.api.ScoreboardRow

interface TeamInfo {
    val id: Int
    val name: String
    val shortName: String
    val contestSystemId: String
    val groups: Set<String>
    val hashTag: String?
    val cdsScoreboardRow: ScoreboardRow?
        get() = null
    val medias: Map<MediaType, String>

    fun toApi() = org.icpclive.api.TeamInfo(
        id,
        name,
        shortName,
        contestSystemId,
        groups.toList(),
        hashTag,
        medias
    )

}