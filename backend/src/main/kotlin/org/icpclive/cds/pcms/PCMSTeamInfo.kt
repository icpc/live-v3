package org.icpclive.cds.pcms

import org.icpclive.api.RunInfo
import org.icpclive.api.TeamInfo

class PCMSTeamInfo(
    val teamInfo: TeamInfo,
    private val hallId: String,
    problemsNumber: Int
) {
    val runs: MutableList<List<RunInfo>> =
        MutableList(problemsNumber) { emptyList() }

    override fun toString(): String {
        return "$hallId. ${teamInfo.shortName}"
    }
}