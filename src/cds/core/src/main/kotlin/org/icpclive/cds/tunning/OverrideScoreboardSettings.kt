package org.icpclive.cds.tunning

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.icpclive.cds.api.*
import org.icpclive.cds.util.serializers.DurationInMinutesSerializer
import kotlin.time.Duration

@Serializable
@SerialName("overrideScoreboardSettings")
public data class OverrideScoreboardSettings(
    @Serializable(with = DurationInMinutesSerializer::class)
    public val penaltyPerWrongAttempt: Duration? = null,
    public val showTeamsWithoutSubmissions: Boolean? = null,
    public val penaltyRoundingMode: PenaltyRoundingMode? = null,
): TuningRule {
    @OptIn(InefficientContestInfoApi::class)
    override fun process(info: ContestInfo, submittedTeams: Set<TeamId>): ContestInfo {
        return info.copy(
            teamList = if (showTeamsWithoutSubmissions != false) info.teamList else info.teamList.filter { it.id in submittedTeams },
            penaltyPerWrongAttempt = penaltyPerWrongAttempt ?: info.penaltyPerWrongAttempt,
            penaltyRoundingMode = penaltyRoundingMode ?: info.penaltyRoundingMode,
        )
    }
}