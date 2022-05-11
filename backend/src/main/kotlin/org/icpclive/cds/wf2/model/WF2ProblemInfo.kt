package org.icpclive.cds.wf2.model

import org.icpclive.cds.ProblemInfo
import java.awt.Color

class WF2ProblemInfo(
    val id: Int,
    letter: String,
    name: String,
    color: Color,
    val testCount: Int?
) : ProblemInfo(letter, name, color) {
    override fun toString(): String {
        return "WF2ProblemInfo(id=$id, letter=$letter name=$name color=${color} testCount=$testCount)"
    }
}
