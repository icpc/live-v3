package org.icpclive.cds.plugins.yandex.api

import kotlinx.serialization.Serializable
import org.icpclive.cds.api.*

@Serializable
internal data class Participant(
    val id: Long,
    val name: String,
    val login: String,
    val uid: String?,
) {
    fun toTeamInfo(loginRegex: Regex) = TeamInfo(
        id = login.toTeamId(),
        fullName = name,
        displayName = name,
        groups = emptyList(),
        hashTag = null,
        medias = emptyMap(),
        isOutOfContest = false,
        isHidden = !loginRegex.matches(login),
        organizationId = null
    )
}
