package org.icpclive.api

import kotlinx.serialization.Serializable
import org.icpclive.util.ColorSerializer
import org.icpclive.util.humanReadable
import java.awt.Color

@Serializable
data class TeamInfoOverride(
    val name: String? = null,
    val shortname: String? = null,
    val groups: List<String>? = null,
    val hashTag: String? = null,
    val medias: Map<TeamMediaType, MediaType?>? = null,
    val additionalInfo: String? = null,
)

@Serializable
data class ProblemInfoOverride(
    val name: String? = null,
    @Serializable(ColorSerializer::class)
    val color: Color? = null,
    val minScore: Double? = null,
    val maxScore: Double? = null,
    val scoreMergeMode: ScoreMergeMode? = null
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
    val freezeTimeSeconds: Long? = null,
    val holdTimeSeconds: Long? = null,
    val teamMediaTemplate: Map<TeamMediaType, MediaType?>? = null,
    val teamOverrides: Map<String, TeamInfoOverride>? = null,
    val problemOverrides: Map<String, ProblemInfoOverride>? = null,
    val scoreboardOverrides: RankingSettings? = null
)

fun ContestInfo.toAdvancedProperties(fields: Set<String>) = AdvancedProperties(
    startTime = startTime.humanReadable.takeIf { "startTime" in fields },
    freezeTimeSeconds = freezeTime.inWholeSeconds.takeIf { "freezeTime" in fields },
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
        it.letter to ProblemInfoOverride(
            name = it.name.takeIf { "problemName" in fields },
            color = it.color.takeIf { "color" in fields },
            minScore = it.minScore.takeIf { "minScore" in fields },
            maxScore = it.maxScore.takeIf { "maxScore" in fields },
            scoreMergeMode = it.scoreMergeMode.takeIf { "scoreMergeMode" in fields }
        )
    },
    scoreboardOverrides = RankingSettings(
        medals = medals.takeIf { "medals" in fields },
        penaltyPerWrongAttempt = penaltyPerWrongAttempt.takeIf { "penaltyPerWrongAttempt" in fields },
        penaltyRoundingMode = penaltyRoundingMode.takeIf { "penaltyRoundingMode" in fields }
    )
)
