package org.icpclive.events.WF

import org.icpclive.events.EventsLoader.Companion.instance
import org.icpclive.events.ProblemInfo
import org.icpclive.events.RunInfo
import org.icpclive.events.TeamInfo
import kotlin.math.max

/**
 * Created by aksenov on 16.04.2015.
 */
class WFRunInfo : RunInfo {
    override var id = 0
    override var isJudged = false
    override var isReallyUnknown = false
    override var result = ""
    var languageId = 0
    override var problemId = 0
    var passedTestsNumber = 0
    var totalTestsNumber = 0
    override var time: Long = 0
    override var lastUpdateTime: Long = 0
        set(value) {
            field = Math.max(field, value) // ?????
        }
    override var teamId = 0
    var team: TeamInfo? = null
    private val passedTests: MutableSet<Int> = HashSet()

    constructor() {}
    constructor(another: WFRunInfo) {
        id = another.id
        isJudged = another.isJudged
        result = another.result
        languageId = another.languageId
        problemId = another.problemId
        teamId = another.teamId
        time = another.time
        passedTestsNumber = another.passedTestsNumber
        totalTestsNumber = another.totalTestsNumber
        lastUpdateTime = another.lastUpdateTime
    }

    fun add(test: WFTestCaseInfo) {
        if (totalTestsNumber == 0) {
            totalTestsNumber = test.total
        }
        passedTests.add(test.id)
        passedTestsNumber = passedTests.size
        lastUpdateTime = max(lastUpdateTime, test.time)
    }

    override val isAccepted: Boolean
        get() = "AC" == result

    // TODO: this should be received from cds
    override val isAddingPenalty: Boolean
        get() =// TODO: this should be received from cds
            isJudged && !isAccepted && "CE" != result
    override val problem: ProblemInfo
        get() = instance.contestData!!.problems[problemId]
    override val percentage: Double
        get() = 1.0 * passedTestsNumber / totalTestsNumber

    override fun toString(): String {
        var teamName = "" + teamId
        if (team != null) teamName = team!!.shortName
        return teamName + " " + ('A'.code + problemId).toChar() + " " + result
    }
}