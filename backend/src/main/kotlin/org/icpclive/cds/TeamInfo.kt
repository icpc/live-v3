package org.icpclive.cds

interface TeamInfo {
    val id: Int
    val rank: Int
    val name: String
    val shortName: String
    val alias: String
    val groups: Set<String>
    val penalty: Int
    val solvedProblemsNumber: Int
    val lastAccepted: Long
    val runs: List<List<RunInfo>>
    fun addRun(run: RunInfo, problem: Int)
    val hashTag: String?
    fun copy(): TeamInfo

    companion object {
        val comparator = compareBy<TeamInfo>(
            { -it.solvedProblemsNumber },
            { it.penalty },
            { it.lastAccepted }
        )
        val strictComparator: Comparator<TeamInfo> = comparator.thenComparing { it: TeamInfo -> it.name }
    }
}