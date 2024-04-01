package org.icpclive.cds.api

import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.serialization.*
import org.icpclive.util.DurationInMillisecondsSerializer
import org.icpclive.util.UnixMillisecondsSerializer
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes


@Serializable
public enum class ContestResultType {
    ICPC, IOI
}

@Serializable
public enum class ContestStatus {
    BEFORE, RUNNING, FAKE_RUNNING, OVER, FINALIZED;

    public companion object {
        public fun byCurrentTime(startTime: Instant, contestLength: Duration): ContestStatus {
            val offset = Clock.System.now() - startTime
            return when {
                offset < Duration.ZERO -> BEFORE
                offset < contestLength -> RUNNING
                else -> OVER
            }
        }
    }
}

@Serializable
public enum class PenaltyRoundingMode {
    /**
     * Round time of all submissions from the beginning of the contest down to whole minute, and then sum them
     */
    @SerialName("each_submission_down_to_minute")
    EACH_SUBMISSION_DOWN_TO_MINUTE,

    /**
     * Round time of all submissions from the beginning of the contest up to whole minute, and then sum them
     */
    @SerialName("each_submission_up_to_minute")
    EACH_SUBMISSION_UP_TO_MINUTE,

    /**
     * Sum time of all submissions from the beginning of the contest and then round it down to whole minute
     */
    @SerialName("sum_down_to_minute")
    SUM_DOWN_TO_MINUTE,

    /**
     * Sum time of all submissions without rounding
     */
    @SerialName("sum_in_seconds")
    SUM_IN_SECONDS,

    /**
     * Get time of last submission as penalty
     */
    @SerialName("last")
    LAST,

    /**
     * Don't have any penalty as a tie-breaker
     */
    @SerialName("zero")
    ZERO,
}

@Target(AnnotationTarget.PROPERTY)
@RequiresOptIn(
    level = RequiresOptIn.Level.ERROR,
    message = "This api is not efficient in most cases, consider using corresponding map instead"
)
public annotation class InefficientContestInfoApi

@Serializable
@OptIn(InefficientContestInfoApi::class)
public data class ContestInfo(
    val name: String,
    val status: ContestStatus,
    val resultType: ContestResultType,
    @SerialName("startTimeUnixMs")
    @Serializable(with = UnixMillisecondsSerializer::class)
    val startTime: Instant,
    @SerialName("contestLengthMs")
    @Serializable(with = DurationInMillisecondsSerializer::class)
    val contestLength: Duration,
    @SerialName("freezeTimeMs")
    @Serializable(with = DurationInMillisecondsSerializer::class)
    val freezeTime: Duration,
    @InefficientContestInfoApi @SerialName("problems") val problemList: List<ProblemInfo>,
    @InefficientContestInfoApi @SerialName("teams") val teamList: List<TeamInfo>,
    @InefficientContestInfoApi @SerialName("groups") val groupList: List<GroupInfo>,
    @InefficientContestInfoApi @SerialName("organizations") val organizationList: List<OrganizationInfo>,
    val penaltyRoundingMode: PenaltyRoundingMode,
    @SerialName("holdBeforeStartTimeMs")
    @Serializable(with = DurationInMillisecondsSerializer::class)
    @Required val holdBeforeStartTime: Duration? = null,
    @Required val emulationSpeed: Double = 1.0,
    @Required val awardsSettings: AwardsSettings = AwardsSettings(),
    @Required val penaltyPerWrongAttempt: Duration = 20.minutes,
    @Transient val cdsSupportsFinalization: Boolean = false,
) {
    public val currentContestTime: Duration
        get() = when (status) {
            ContestStatus.BEFORE -> Duration.ZERO
            ContestStatus.RUNNING, ContestStatus.FAKE_RUNNING -> (Clock.System.now() - startTime) * emulationSpeed
            ContestStatus.OVER, ContestStatus.FINALIZED -> contestLength
        }
    val groups: Map<GroupId, GroupInfo> by lazy { groupList.associateBy { it.id } }
    val teams: Map<TeamId, TeamInfo> by lazy { teamList.associateBy { it.id } }
    val organizations: Map<OrganizationId, OrganizationInfo> by lazy { organizationList.associateBy { it.id } }
    val problems: Map<ProblemId, ProblemInfo> by lazy { problemList.associateBy { it.id } }
    val scoreboardProblems: List<ProblemInfo> by lazy { problemList.sortedBy { it.ordinal }.filterNot { it.isHidden } }
}
