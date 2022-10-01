package org.icpclive.api

import kotlinx.serialization.Serializable

@Serializable
data class TeamInfoOverride(
    val name: String? = null,
    val shortname: String? = null,
    val groups: List<String>? = null,
    val hashTag: String? = null,
    val medias: Map<TeamMediaType, MediaType?>? = null,
)

@Serializable
data class ProblemInfoOverride(
    val name: String? = null,
    val color: String? = null,
)

@Serializable
class RankingSettings(
    val medals: List<MedalType>? = null,
    val penaltyPerWrongAttempt: Int? = null,
    val showTeamsWithoutSubmissions: Boolean? = null
)

@Serializable
data class AdvancedProperties(
    val startTime: String? = null,
    val holdTimeSeconds: String? = null,
    val teamMediaTemplate: Map<TeamMediaType, MediaType?>? = null,
    val teamOverrides: Map<String, TeamInfoOverride>? = null,
    val problemOverrides: Map<String, ProblemInfoOverride>? = null,
    val scoreboardOverrides: RankingSettings? = null
)
