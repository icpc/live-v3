package org.icpclive.cds.tunning

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.icpclive.cds.api.*
import org.icpclive.cds.util.serializers.DurationInMinutesSerializer
import kotlin.time.Duration

/**
 * The rule customizing scoreboard calculation rules.
 *
 * All fields can be null, existing values are not changed in that case.
 *
 * @param penaltyPerWrongAttempt how much penalty time wrong attempt cost for a team
 * @param showTeamsWithoutSubmissions should teams without submissions be shown in the scoreboard?
 * @param penaltyRoundingMode specifies rules on how penalty time is calculated from times of submissions
 * @param problemColorPolicy specifies when problem colors are shown
 */
@Serializable
@SerialName("overrideScoreboardSettings")
public data class OverrideScoreboardSettings(
    @Serializable(with = DurationInMinutesSerializer::class)
    public val penaltyPerWrongAttempt: Duration? = null,
    public val showTeamsWithoutSubmissions: Boolean? = null,
    public val penaltyRoundingMode: PenaltyRoundingMode? = null,
    public val problemColorPolicy: ProblemColorPolicy? = null,
): TuningRule {
    @OptIn(InefficientContestInfoApi::class)
    override fun process(info: ContestInfo): ContestInfo {
        return info.copy(
            penaltyPerWrongAttempt = penaltyPerWrongAttempt ?: info.penaltyPerWrongAttempt,
            penaltyRoundingMode = penaltyRoundingMode ?: info.penaltyRoundingMode,
            showTeamsWithoutSubmissions = showTeamsWithoutSubmissions ?: info.showTeamsWithoutSubmissions,
            problemColorPolicy = problemColorPolicy ?: info.problemColorPolicy
        )
    }
}