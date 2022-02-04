package org.icpclive.cds.pcms

import org.icpclive.api.ICPCProblemResult
import org.icpclive.api.ProblemResult
import org.icpclive.api.ScoreboardRow
import org.icpclive.cds.RunInfo
import org.icpclive.cds.TeamInfo
import java.lang.Integer.min
import kotlin.math.max

class PCMSTeamInfo(
    override val id: Int,
    override val alias: String,
    val hallId: String,
    override val name: String,
    override val shortName: String,
    override val hashTag: String?,
    override val groups: Set<String>,
    problemsNumber: Int,
    val delay: Int
) : TeamInfo {
    override val runs: MutableList<List<PCMSRunInfo>> =
        MutableList(problemsNumber) { emptyList() }

    override fun copy(): PCMSTeamInfo {
        return PCMSTeamInfo(
            id, alias, hallId, name, shortName, hashTag,
            groups, runs.size, delay
        )
    }

    override fun toString(): String {
        return "$hallId. $shortName"
    }

    override var rank: Int = 1
    override var solvedProblemsNumber = 0
    override var penalty = 0
    override var lastAccepted: Long = 0
}