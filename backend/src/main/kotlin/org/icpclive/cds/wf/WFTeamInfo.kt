package org.icpclive.cds.wf

import org.icpclive.cds.RunInfo
import org.icpclive.cds.TeamInfo

/**
 * Created by Meepo on 3/5/2018.
 */
open class WFTeamInfo(problems: Int) : TeamInfo {
    protected var problem_runs: ArrayList<ArrayList<RunInfo>>
    override var id = -1
    override var rank = 0
    override lateinit var name: String
    override var solvedProblemsNumber = 0
    override var penalty = 0
    override var lastAccepted: Long = 0
    override lateinit var groups: HashSet<String>
    override lateinit var shortName: String
    override var hashTag: String? = null

    init {
        problem_runs = ArrayList(problems)
        for (i in 0 until problems) {
            problem_runs.add(ArrayList())
        }
        groups = HashSet()
    }

    constructor(teamInfo: WFTeamInfo) : this(teamInfo.runs.size) {
        id = teamInfo.id
        rank = teamInfo.rank
        name = teamInfo.name
        groups = HashSet(teamInfo.groups)
        shortName = teamInfo.shortName
    }

    override fun copy(): WFTeamInfo {
        val teamInfo = WFTeamInfo(problem_runs.size)
        teamInfo.id = id
        teamInfo.rank = rank
        teamInfo.name = name
        teamInfo.groups = HashSet(groups)
        teamInfo.shortName = shortName
        return teamInfo
    }

    override val alias: String
        get() = (id + 1).toString() + ""
    override val runs: List<List<RunInfo>>
        get() = problem_runs

    fun addRun(run: RunInfo, problemId: Int) {
        val runs = problem_runs[problemId]
        synchronized(runs!!) { runs.add(run) }
    }

    override fun toString(): String {
        return String.format("%03d", id + 1) + ". " + shortName
    }
}