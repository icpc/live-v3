package org.icpclive.events.codeforces

import org.icpclive.events.ProblemInfo
import org.icpclive.events.codeforces.api.data.CFProblem
import kotlin.math.max

/**
 * @author egor@egork.net
 */
class CFProblemInfo(val problem: CFProblem, val id: Int) : ProblemInfo(problem.index, problem.name) {
    var totalTests = 1
        private set

    fun update(run: CFRunInfo) {
        totalTests = max(totalTests, run.submission.passedTestCount + if (run.isAccepted) 0 else 1)
    }
}