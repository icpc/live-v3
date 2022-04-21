package org.icpclive.cds.wf

import org.icpclive.api.MediaType
import org.icpclive.cds.RunInfo
import org.icpclive.cds.TeamInfo

/**
 * Created by Meepo on 3/5/2018.
 */
open class WFTeamInfo(problems: Int) : TeamInfo {
    protected var problem_runs: ArrayList<ArrayList<RunInfo>>
    override var id = -1
    override lateinit var name: String
    override lateinit var groups: HashSet<String>
    override lateinit var shortName: String
    override var hashTag: String? = null
    override val medias: Map<MediaType, String> = emptyMap()

    init {
        problem_runs = ArrayList(problems)
        for (i in 0 until problems) {
            problem_runs.add(ArrayList())
        }
        groups = HashSet()
    }

    constructor(teamInfo: WFTeamInfo) : this(teamInfo.runs.size) {
        id = teamInfo.id
        name = teamInfo.name
        groups = HashSet(teamInfo.groups)
        shortName = teamInfo.shortName
    }

    override val contestSystemId: String
        get() = (id + 1).toString() + ""
    val runs: List<List<RunInfo>>
        get() = problem_runs

    fun addRun(run: RunInfo, problemId: Int) {
        val runs = problem_runs[problemId]
        synchronized(runs) { runs.add(run) }
    }

    override fun toString(): String {
        return String.format("%03d", id + 1) + ". " + shortName
    }
}