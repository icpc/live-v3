package org.icpclive.events.WF.json

import org.icpclive.events.ProblemInfo

/**
 * Created by Aksenov239 on 3/5/2018.
 */
class WFProblemInfo(languages: Int) : ProblemInfo() {
    @JvmField
    var id = 0
    @JvmField
    var testCount = 0
    @JvmField
    var submissions: IntArray

    init {
        submissions = IntArray(languages)
    }
}