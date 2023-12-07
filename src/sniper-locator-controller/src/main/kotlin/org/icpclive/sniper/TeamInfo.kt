package org.icpclive.sniper

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.icpclive.api.MediaType
import org.icpclive.api.TeamMediaType

@Serializable
data class TeamInfo(
    val id: Int,
    @SerialName("name") val fullName: String,
    @SerialName("shortName") val displayName: String,
    val contestSystemId: String,
    val groups: List<String>,
    val hashTag: String?,
    val medias: Map<TeamMediaType, MediaType>,
    val isHidden: Boolean,
    val isOutOfContest: Boolean,
    val organizationId: String? = null,
    val customFields: Map<String, String> = emptyMap(),
)