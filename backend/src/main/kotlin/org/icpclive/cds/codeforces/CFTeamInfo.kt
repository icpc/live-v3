package org.icpclive.cds.codeforces

import org.icpclive.cds.RunInfo
import org.icpclive.cds.TeamInfo
import org.icpclive.cds.codeforces.api.data.CFRanklistRow

/**
 * @author egor@egork.net
 */
class CFTeamInfo(private val row: CFRanklistRow) : TeamInfo {
    override var id = 0

    override val name: String
        get() = if (row.party.teamName != null) {
            row.party.teamName
        } else row.party.members[0].handle
    override val shortName: String
        get() = name
    override val alias: String
        get() = name
    override val groups: Set<String>
        get() = emptySet()
    val runs: List<List<CFRunInfo>>
        get() = CFEventsLoader.instance.contestData.getRuns(row.party)

    override val hashTag: String
        get() = ""
}