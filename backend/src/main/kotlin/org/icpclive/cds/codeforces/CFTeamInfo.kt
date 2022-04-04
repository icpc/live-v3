package org.icpclive.cds.codeforces

import org.icpclive.api.MediaType
import org.icpclive.cds.TeamInfo
import org.icpclive.cds.codeforces.api.data.CFRankListRow

/**
 * @author egor@egork.net
 */
class CFTeamInfo(private val row: CFRankListRow) : TeamInfo {
    override var id = 0

    override val name: String
        get() = row.party.teamName ?: row.party.members[0].let { it.name ?: it.handle }
    override val shortName: String
        get() = name
    override val alias: String
        get() = name
    override val groups = emptySet<String>()
    override val hashTag: String? = null
    override val medias = emptyMap<MediaType, String>()
}