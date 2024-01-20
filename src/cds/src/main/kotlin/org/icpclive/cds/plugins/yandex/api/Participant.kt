package org.icpclive.cds.plugins.yandex.api

import kotlinx.serialization.Serializable
import org.icpclive.api.TeamInfo

@Serializable
internal data class Participant(
    val id: Int,
    val name: String,
    val login: String,
    val uid: String?
) {
    fun toTeamInfo() = TeamInfo(
        id = id,
        fullName = name,
        displayName = name,
        contestSystemId = login,
        groups = emptyList(),
        hashTag = null,
        medias = emptyMap(),
        isOutOfContest = false,
        isHidden = false,
        organizationId = null
    )
}
