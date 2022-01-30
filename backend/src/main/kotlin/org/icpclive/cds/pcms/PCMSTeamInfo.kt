package org.icpclive.cds.pcms

import org.icpclive.cds.RunInfo
import org.icpclive.cds.TeamInfo
import java.lang.Integer.min

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
    constructor(pcmsTeamInfo: PCMSTeamInfo) : this(
        pcmsTeamInfo.id, pcmsTeamInfo.alias, pcmsTeamInfo.hallId, pcmsTeamInfo.name,
        pcmsTeamInfo.shortName, pcmsTeamInfo.hashTag, pcmsTeamInfo.groups, pcmsTeamInfo.runs.size,
        pcmsTeamInfo.delay
    ) {
        for (i in pcmsTeamInfo.runs.indices) {
            runs[i].addAll(pcmsTeamInfo.runs[i])
        }
    }

    override fun addRun(run: RunInfo, problem: Int) {
        runs[problem].add(run as PCMSRunInfo)
    }

    fun mergeRuns(runs: ArrayList<PCMSRunInfo>, problemId: Int, lastRunId: Int, currentTime: Long): Int {
        var lastRunId = lastRunId
        val previousSize = this.runs[problemId].size
        for (i in 0 until min(previousSize,  runs.size)) {
            val pcmsRun = this.runs[problemId][i]
            if (!pcmsRun.isJudged && runs[i].isJudged) {
                pcmsRun.lastUpdateTime = currentTime
                pcmsRun.result = runs[i].result
                pcmsRun.isJudged = true
            }
        }
        for (i in previousSize until runs.size) {
            runs[i].id = lastRunId++
            this.runs[problemId].add(runs[i])
        }
        return lastRunId
    }

    override val runs: List<MutableList<PCMSRunInfo>> =
        List(problemsNumber) { mutableListOf() }

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