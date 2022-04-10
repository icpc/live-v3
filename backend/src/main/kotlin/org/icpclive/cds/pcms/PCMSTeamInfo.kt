package org.icpclive.cds.pcms

import org.icpclive.api.MediaType
import org.icpclive.api.RunInfo
import org.icpclive.cds.TeamInfo

class PCMSTeamInfo(
    override val id: Int,
    override val alias: String,
    private val hallId: String,
    override val name: String,
    override val shortName: String,
    override val hashTag: String?,
    override val groups: Set<String>,
    override val medias: Map<MediaType, String>,
    problemsNumber: Int
) : TeamInfo {
    val runs: MutableList<List<RunInfo>> =
        MutableList(problemsNumber) { emptyList() }

    override fun toString(): String {
        return "$hallId. $shortName"
    }
}