@file:Suppress("UNUSED")
package org.icpclive.api

import kotlinx.serialization.*
import org.icpclive.cds.api.*
import org.icpclive.cds.util.datetime.DurationInMillisecondsSerializer
import kotlin.time.Duration

@Serializable
class ExternalRunInfo(
    val id: String,
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
    val id: String,
    val fullName: String,
    val displayName: String,
    val groups: List<GroupInfo>,
    val hashTag: String?,
    val medias: Map<TeamMediaType, MediaType>,
    val isOutOfContest: Boolean,
    val scoreboardRowBefore: ScoreboardRow,
    val rankBefore: Int,
    val scoreboardRowAfter: ScoreboardRow,
    val rankAfter: Int,
    @Required val organization: OrganizationInfo?,
    @Required val customFields: Map<String, String> = emptyMap(),
)