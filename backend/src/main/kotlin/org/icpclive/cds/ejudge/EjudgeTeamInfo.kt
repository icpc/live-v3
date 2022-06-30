package org.icpclive.cds.ejudge

import org.icpclive.api.RunInfo
import org.icpclive.api.TeamInfo
import java.util.*

/**
 * @author Mike Perveev
 */
class EjudgeTeamInfo(
    val teamInfo: TeamInfo,
    problemsNumber: Int
) {
    val runs: MutableList<MutableMap<Int, RunInfo>> = MutableList(problemsNumber) { TreeMap() }

    override fun toString(): String {
        return teamInfo.shortName
    }
}