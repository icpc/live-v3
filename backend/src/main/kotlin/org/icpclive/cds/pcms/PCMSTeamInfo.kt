package org.icpclive.cds.pcms

import org.icpclive.cds.TeamInfo

class PCMSTeamInfo(
    override val id: Int,
    override val alias: String,
    private val hallId: String,
    override val name: String,
    override val shortName: String,
    override val hashTag: String?,
    override val groups: Set<String>,
    problemsNumber: Int
) : TeamInfo {
    val runs: MutableList<List<PCMSRunInfo>> =
        MutableList(problemsNumber) { emptyList() }

    override fun toString(): String {
        return "$hallId. $shortName"
    }
}