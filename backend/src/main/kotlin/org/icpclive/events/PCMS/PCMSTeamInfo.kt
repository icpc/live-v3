package org.icpclive.events.PCMS

import org.icpclive.events.RunInfo
import org.icpclive.events.TeamInfo

class PCMSTeamInfo(problemsNumber: Int) : TeamInfo {
    override lateinit var alias: String

    constructor(
        id: Int, alias: String, hallId: String, name: String, shortName: String, hashTag: String?,
        groups: Set<String>, problemsNumber: Int, delay: Int
    ) : this(problemsNumber) {
        this.id = id
        this.alias = alias
        this.hallId = hallId
        this.name = name
        this.shortName = shortName
        this.groups = groups
        this.hashTag = hashTag
        this.delay = delay
    }

    constructor(name: String, problemsNumber: Int) : this(-1, "", "1", name, "", null, emptySet(), problemsNumber, 0) {}
    constructor(pcmsTeamInfo: PCMSTeamInfo) : this(
        pcmsTeamInfo.id, pcmsTeamInfo.alias, pcmsTeamInfo.hallId, pcmsTeamInfo.name,
        pcmsTeamInfo.shortName, pcmsTeamInfo.hashTag, pcmsTeamInfo.groups, pcmsTeamInfo.problemRuns.size,
        pcmsTeamInfo.delay
    ) {
        for (i in pcmsTeamInfo.problemRuns.indices) {
            problemRuns[i]!!.addAll(pcmsTeamInfo.problemRuns[i]!!)
        }
    }

    override fun addRun(run: RunInfo, problemId: Int) {
        if (run != null) {
            problemRuns[problemId]!!.add(run)
        }
    }

    fun mergeRuns(runs: ArrayList<PCMSRunInfo>, problemId: Int, lastRunId: Int, currentTime: Long): Int {
        var lastRunId = lastRunId
        val previousSize = problemRuns[problemId]!!.size
        var i = 0
        while (i < previousSize && i < runs.size) {
            val pcmsRun = problemRuns[problemId]!![i] as PCMSRunInfo
            if (!pcmsRun.isJudged && runs[i].isJudged) {
                pcmsRun.lastUpdateTime = currentTime
                pcmsRun.result = runs[i].result
                pcmsRun.isJudged = true
            }
            i++
        }
        for (i in previousSize until runs.size) {
            runs[i].id = lastRunId++
            problemRuns[problemId]!!.add(runs[i])
        }
        return lastRunId
    }

    fun getRunsNumber(problemId: Int): Int {
        return problemRuns[problemId]!!.size
    }

    fun getLastSubmitTime(problemId: Int): Long {
        val runsNumber = getRunsNumber(problemId)
        return if (runsNumber == 0) -1 else problemRuns[problemId]!![runsNumber].time
    }

    override val runs: List<List<RunInfo>>
        get() = problemRuns

    override fun copy(): PCMSTeamInfo {
        return PCMSTeamInfo(
            id, alias, hallId, name, shortName, hashTag,
            groups, problemRuns.size, delay
        )
    }

    override fun toString(): String {
        return "$hallId. $shortName"
    }

    override var id: Int = 0
    lateinit var hallId: String
    override lateinit var name: String
    override lateinit var shortName: String
    override lateinit var groups: Set<String>
    override var hashTag: String? = null
    override var rank: Int = 0
    override var solvedProblemsNumber = 0
    override var penalty = 0
    override var lastAccepted: Long = 0
    var problemRuns: ArrayList<ArrayList<RunInfo>>
    var delay: Int = 0

    init {
        problemRuns = ArrayList(problemsNumber)
        for (i in 0 until problemsNumber) {
            problemRuns.add(ArrayList())
        }
        rank = 1
    }
}