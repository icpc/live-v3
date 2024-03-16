package org.icpclive.cds.api

import kotlinx.serialization.*

@JvmInline
@Serializable
public value class TeamId(public val value: String) {
    override fun toString(): String = value
}

@Serializable
public enum class TeamMediaType {
    @SerialName("camera")
    CAMERA,

    @SerialName("screen")
    SCREEN,

    @SerialName("record")
    RECORD,

    @SerialName("photo")
    PHOTO,

    @SerialName("reactionVideo")
    REACTION_VIDEO,

    @SerialName("achievement")
    ACHIEVEMENT,
}

@Serializable
public data class TeamInfo(
    val id: TeamId,
    @SerialName("name") val fullName: String,
    @SerialName("shortName") val displayName: String,
    val groups: List<GroupId>,
    val hashTag: String?,
    val medias: Map<TeamMediaType, MediaType>,
    val isHidden: Boolean,
    val isOutOfContest: Boolean,
    val organizationId: OrganizationId?,
    @Required val customFields: Map<String, String> = emptyMap(),
)