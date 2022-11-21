package org.icpclive.api

import kotlinx.serialization.Serializable
import org.icpclive.util.humanReadable

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
    val showTeamsWithoutSubmissions: Boolean? = null,
    val penaltyRoundingMode: PenaltyRoundingMode? = null
)

@Serializable
data class AdvancedProperties(
    val startTime: String? = null,
    val holdTimeSeconds: Long? = null,
    val teamMediaTemplate: Map<TeamMediaType, MediaType?>? = null,
    val teamOverrides: Map<String, TeamInfoOverride>? = null,
    val problemOverrides: Map<String, ProblemInfoOverride>? = null,
    val scoreboardOverrides: RankingSettings? = null
)

fun ContestInfo.toAdvancedProperties(fields: Set<String>) = AdvancedProperties(
    startTime = startTime.humanReadable.takeIf { "startTime" in fields },
    holdTimeSeconds = holdBeforeStartTime?.inWholeSeconds?.takeIf { "holdBeforeStartTime" in fields},
    teamOverrides = teams.associate {
        it.contestSystemId to TeamInfoOverride(
            name = it.name.takeIf { "name" in fields },
            shortname = it.shortName.takeIf { "shortname" in fields },
            groups = it.groups.takeIf { "groups" in fields },
            hashTag = it.hashTag.takeIf { "hashTag" in fields},
            medias = it.medias.takeIf { "medias" in fields }
        )
    },
    problemOverrides = problems.associate {
        val colorString = (it.color.rgb and 0xffffff).toString(radix = 16).padStart(6, '0')
        it.letter to ProblemInfoOverride(
            name = it.name.takeIf { "problemName" in fields },
            color = "#$colorString".takeIf { "color" in fields }
        )
    },
    scoreboardOverrides = RankingSettings(
        medals = medals.takeIf { "medals" in fields },
        penaltyPerWrongAttempt = penaltyPerWrongAttempt.takeIf { "penaltyPerWrongAttempt" in fields },
        penaltyRoundingMode = penaltyRoundingMode.takeIf { "penaltyRoundingMode" in fields }
    )
)