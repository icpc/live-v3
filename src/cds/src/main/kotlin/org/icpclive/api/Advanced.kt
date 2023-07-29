package org.icpclive.api

import kotlinx.datetime.Instant
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.icpclive.util.*
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
    val isHidden: Boolean? = null,
    val isOutOfContest: Boolean? = null
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
data class GroupInfoOverride(
    val isHidden: Boolean? = null,
    val isOutOfContest: Boolean? = null
)

@Serializable
class RankingSettings(
    val medals: List<MedalType>? = null,
    @Serializable(with = DurationInMinutesSerializer::class)
    val penaltyPerWrongAttempt: Duration? = null,
    val showTeamsWithoutSubmissions: Boolean? = null,
    val penaltyRoundingMode: PenaltyRoundingMode? = null
)

@Serializable
data class AdvancedProperties(
    @Serializable(with = HumanTimeSerializer::class)
    val startTime: Instant? = null,
    @Serializable(with = DurationInSecondsSerializer::class)
    @SerialName("freezeTimeSeconds")
    val freezeTime: Duration? = null,
    @Serializable(with = DurationInSecondsSerializer::class)
    @SerialName("holdTimeSeconds")
    val holdTime: Duration? = null,
    val teamMediaTemplate: Map<TeamMediaType, MediaType?>? = null,
    val teamOverrides: Map<String, TeamInfoOverride>? = null,
    val groupOverrides: Map<String, GroupInfoOverride>? = null,
    val problemOverrides: Map<String, ProblemInfoOverride>? = null,
    val scoreboardOverrides: RankingSettings? = null
)

fun ContestInfo.toAdvancedProperties(fields: Set<String>) : AdvancedProperties {
    fun <T> T.takeIfAsked(name: String) = takeIf { name in fields || "all" in fields }
    return AdvancedProperties(
        startTime = startTime.takeIfAsked("startTime"),
        freezeTime = freezeTime.takeIfAsked("freezeTime"),
        holdTime = holdBeforeStartTime?.takeIfAsked("holdBeforeStartTime"),
        teamOverrides = teams.associate {
            it.contestSystemId to TeamInfoOverride(
                name = it.name.takeIfAsked("name"),
                shortname = it.shortName.takeIfAsked("shortname"),
                groups = it.groups.takeIfAsked("groups"),
                hashTag = it.hashTag.takeIfAsked("hashTag"),
                medias = it.medias.takeIfAsked("medias"),
                isHidden = it.isHidden.takeIfAsked("isHidden"),
                isOutOfContest = it.isOutOfContest.takeIfAsked("isOutOfContest")
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
        groupOverrides = groups.associate {
            it.name to GroupInfoOverride(
                isHidden = it.isHidden.takeIfAsked("isHidden"),
                isOutOfContest = it.isOutOfContest.takeIfAsked("isOutOfContest")
            )
        },
        scoreboardOverrides = RankingSettings(
            medals = medals.takeIfAsked("medals"),
            penaltyPerWrongAttempt = penaltyPerWrongAttempt.takeIfAsked("penaltyPerWrongAttempt"),
            penaltyRoundingMode = penaltyRoundingMode.takeIfAsked("penaltyRoundingMode")
        )
    )
}