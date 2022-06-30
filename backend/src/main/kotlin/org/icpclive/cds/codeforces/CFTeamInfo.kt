package org.icpclive.cds.codeforces

import org.icpclive.cds.codeforces.api.data.CFRankListRow

class CFTeamInfo(private val row: CFRankListRow) {
    var id = 0

    fun toApi() = org.icpclive.api.TeamInfo(
        id = id,
        name = row.party.teamName ?: row.party.members[0].let { it.name ?: it.handle },
        shortName = row.party.teamName ?: row.party.members[0].let { it.name ?: it.handle },
        contestSystemId = row.party.teamName ?: row.party.members[0].handle,
        groups = emptyList(),
        hashTag = null,
        medias = emptyMap()
    )

}