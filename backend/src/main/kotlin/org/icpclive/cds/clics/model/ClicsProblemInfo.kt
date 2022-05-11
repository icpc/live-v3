package org.icpclive.cds.clics.model

import org.icpclive.cds.ProblemInfo
import java.awt.Color

class ClicsProblemInfo(
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
