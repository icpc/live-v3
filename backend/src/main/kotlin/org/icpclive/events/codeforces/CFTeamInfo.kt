package org.icpclive.events.codeforces

import org.icpclive.events.RunInfo
import org.icpclive.events.TeamInfo
import org.icpclive.events.codeforces.api.data.CFRanklistRow

/**
 * @author egor@egork.net
 */
class CFTeamInfo(private val row: CFRanklistRow) : TeamInfo {
    override var id = 0

    override val rank: Int
        get() = row.rank
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
    override val penalty: Int
        get() = if (row.penalty == 0) row.points.toInt() else row.penalty
    val points: Int
        get() = row.points.toInt()
    override val solvedProblemsNumber: Int
        get() {
            var solved = 0
            for (result in row.problemResults) {
                solved += if (result.points > 0) 1 else 0
            }
            return solved
        }
    override val lastAccepted: Long
        get() {
            var last: Long = 0
            for (result in row.problemResults) {
                if (result.points > 0) {
                    last = Math.max(last, result.bestSubmissionTimeSeconds)
                }
            }
            return last
        }
    override val runs: List<List<CFRunInfo>>
        get() = CFEventsLoader.instance.contestData.getRuns(row.party)

    override fun addRun(run: RunInfo, problem: Int) {
        CFEventsLoader.instance.contestData.addRun(run as CFRunInfo, problem)
    }

    override val hashTag: String
        get() = ""

    override fun copy(): TeamInfo {
        return CFTeamInfo(row)
    }
}