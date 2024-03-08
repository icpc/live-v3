@file:Suppress("UNUSED")
package org.icpclive.api

import kotlinx.serialization.*
import org.icpclive.cds.api.*
import org.icpclive.util.DurationInMillisecondsSerializer
import kotlin.time.Duration

@Serializable
class ExternalRunInfo(
    val id: Int,
    val result: RunResult,
    val problem: ProblemInfo,
    val team: ExternalTeamInfo,
    @Serializable(with = DurationInMillisecondsSerializer::class)
    val time: Duration,
    @Required val featuredRunMedia: MediaType? = null,
    @Required val reactionVideos: List<MediaType> = emptyList(),
)

@Serializable
class ExternalTeamInfo(
    val fullName: String,
    val displayName: String,
    val contestSystemId: String,
    val groups: List<GroupInfo>,
    val hashTag: String?,
    val medias: Map<TeamMediaType, MediaType>,
    val isOutOfContest: Boolean,
    @Required val organization: OrganizationInfo?,
    @Required val customFields: Map<String, String> = emptyMap(),
)