package org.icpclive.events.WF.json

import org.icpclive.events.ProblemInfo

/**
 * Created by Aksenov239 on 3/5/2018.
 */
class WFProblemInfo(languages: Int) : ProblemInfo() {
    var id = 0
    var testCount = 0
    var submissions = IntArray(languages)
}