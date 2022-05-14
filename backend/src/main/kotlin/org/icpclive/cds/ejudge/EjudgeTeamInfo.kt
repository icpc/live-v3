package org.icpclive.cds.ejudge

import org.icpclive.api.MediaType
import org.icpclive.api.RunInfo
import org.icpclive.cds.TeamInfo
import java.util.TreeMap

/**
 * @author Mike Perveev
 */
class EjudgeTeamInfo(
    override val id: Int,
    override val name: String,
    override val shortName: String,
    override val contestSystemId: String,
    override val groups: Set<String>,
    override val hashTag: String?,
    override val medias: Map<MediaType, String>,
    problemsNumber: Int
) : TeamInfo {
    val runs: MutableList<MutableMap<Int, RunInfo>> = MutableList(problemsNumber) { TreeMap() }

    override fun toString(): String {
        return shortName
    }
}