package org.icpclive.api

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.icpclive.util.ColorSerializer
import org.icpclive.util.DurationInSecondsSerializer
import org.icpclive.util.humanReadable
import java.awt.Color
import kotlin.time.Duration

@Serializable
data class TeamInfoOverride(
    val name: String? = null,
    val shortname: String? = null,
    val groups: List<String>? = null,
    val hashTag: String? = null,
    val medias: Map<TeamMediaType, MediaType?>? = null,
    val additionalInfo: String? = null,
    val isHidden: Boolean? = null
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
    @Serializable(with = DurationInSecondsSerializer::class)
    @SerialName("freezeTimeSeconds")
    val freezeTime: Duration? = null,
    @Serializable(with = DurationInSecondsSerializer::class)
    @SerialName("holdTimeSeconds")
    val holdTime: Duration? = null,
    val teamMediaTemplate: Map<TeamMediaType, MediaType?>? = null,
    val teamOverrides: Map<String, TeamInfoOverride>? = null,
    val problemOverrides: Map<String, ProblemInfoOverride>? = null,
    val scoreboardOverrides: RankingSettings? = null
)

fun ContestInfo.toAdvancedProperties(fields: Set<String>) : AdvancedProperties {
    fun <T> T.takeIfAsked(name: String) = takeIf { name in fields || "all" in fields }
    return AdvancedProperties(
        startTime = startTime.humanReadable.takeIfAsked("startTime"),
        freezeTime = freezeTime.takeIfAsked("freezeTime"),
        holdTime = holdBeforeStartTime?.takeIfAsked("holdBeforeStartTime"),
        teamOverrides = teams.associate {
            it.contestSystemId to TeamInfoOverride(
                name = it.name.takeIfAsked("name"),
                shortname = it.shortName.takeIfAsked("shortname"),
                groups = it.groups.takeIfAsked("groups"),
                hashTag = it.hashTag.takeIfAsked("hashTag"),
                medias = it.medias.takeIfAsked("medias"),
                isHidden = it.isHidden.takeIfAsked("isHidden")
            )
        },
        problemOverrides = problems.associate {
            it.letter to ProblemInfoOverride(
                name = it.name.takeIfAsked("problemName"),
                color = it.color.takeIfAsked("color"),
                minScore = it.minScore.takeIfAsked("minScore"),
                maxScore = it.maxScore.takeIfAsked("maxScore"),
                scoreMergeMode = it.scoreMergeMode.takeIfAsked("scoreMergeMode")
            )
        },
        scoreboardOverrides = RankingSettings(
            medals = medals.takeIfAsked("medals"),
            penaltyPerWrongAttempt = penaltyPerWrongAttempt.takeIfAsked("penaltyPerWrongAttempt"),
            penaltyRoundingMode = penaltyRoundingMode.takeIfAsked("penaltyRoundingMode")
        )
    )
}