package org.icpclive.cds

import org.icpclive.api.ScoreboardRow

interface TeamInfo {
    val id: Int
    val name: String
    val shortName: String
    val alias: String
    val groups: Set<String>
    val hashTag: String?
    val cdsScoreboardRow: ScoreboardRow?
        get() = null

    fun toApi() = org.icpclive.api.TeamInfo(
        id,
        name,
        shortName,
        alias,
        groups.toList(),
        hashTag
    )

}