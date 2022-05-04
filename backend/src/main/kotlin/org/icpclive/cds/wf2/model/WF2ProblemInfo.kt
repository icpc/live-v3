package org.icpclive.cds.wf2.model

import org.icpclive.cds.ProblemInfo
import java.awt.Color

class WF2ProblemInfo(
    val id: Int,
    letter: String,
    name: String,
    color: Color,
    val testCount: Int?
) : ProblemInfo(letter, name, color)
