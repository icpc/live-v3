package org.icpclive.cds.api

import kotlinx.serialization.*
import kotlinx.serialization.json.Json
import org.icpclive.cds.util.serializers.*
import kotlin.time.*
import kotlin.time.Duration.Companion.minutes


@Serializable
public enum class ContestResultType {
    ICPC, IOI
}

@Serializable
public sealed class ContestStatus {
    @Serializable
    @SerialName("before")
    public data class BEFORE(
        @SerialName("holdTimeMs")
        @Serializable(with = DurationInMillisecondsSerializer::class)
        public val holdTime: Duration? = null,
        @SerialName("scheduledStartAtUnixMs")
        @Serializable(with = UnixMillisecondsSerializer::class)
        public val scheduledStartAt: Instant? = null
    ) : ContestStatus()
    @Serializable
    @SerialName("running")
    public data class RUNNING(
        @SerialName("startedAtUnixMs")
        @Serializable(with = UnixMillisecondsSerializer::class)
        public val startedAt: Instant,
        @SerialName("frozenAtUnixMs")
        @Serializable(with = UnixMillisecondsSerializer::class)
        public val frozenAt: Instant? = null,
        @Transient public val isFake: Boolean = false
    ) : ContestStatus()
    @Serializable
    @SerialName("over")
    public data class OVER(
        @SerialName("startedAtUnixMs")
        @Serializable(with = UnixMillisecondsSerializer::class)
        public val startedAt: Instant,
        @SerialName("finishAtUnixMs")
        @Serializable(with = UnixMillisecondsSerializer::class)
        public val finishedAt: Instant,
        @SerialName("frozenAtUnixMs")
        @Serializable(with = UnixMillisecondsSerializer::class)
        public val frozenAt: Instant? = null,
    ) : ContestStatus()
    @Serializable
    @SerialName("finalized")
    public data class FINALIZED(
        @SerialName("startedAtUnixMs")
        @Serializable(with = UnixMillisecondsSerializer::class)
        public val startedAt: Instant,
        @SerialName("finishAtUnixMs")
        @Serializable(with = UnixMillisecondsSerializer::class)
        public val finishedAt: Instant,
        @SerialName("finalizedAtUnixMs")
        @Serializable(with = UnixMillisecondsSerializer::class)
        public val finalizedAt: Instant,
        @SerialName("frozenAtUnixMs")
        @Serializable(with = UnixMillisecondsSerializer::class)
        public val frozenAt: Instant? = null,
    ) : ContestStatus()

    final override fun toString(): String = Json.encodeToString(this)

    public companion object {
        public fun byCurrentTime(
            startTime: Instant?,
            freezeTime: Duration?,
            contestLength: Duration,
            holdTime: Duration?,
        ): ContestStatus {
            if (holdTime != null || startTime == null) {
                return BEFORE(holdTime = holdTime, scheduledStartAt = startTime)
            }
            val offset = Clock.System.now() - startTime
            return when {
                offset < Duration.ZERO -> BEFORE(
                    holdTime = null,
                    scheduledStartAt = startTime
                )
                offset < contestLength -> RUNNING(
                    startedAt = startTime,
                    frozenAt = if (freezeTime != null) (startTime + freezeTime).takeIf { offset >= freezeTime } else null,
                )
                else -> OVER(
                    startedAt = startTime,
                    finishedAt = startTime + contestLength,
                    frozenAt = if (freezeTime != null) (startTime + freezeTime).takeIf { offset >= freezeTime } else null,
                )
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
public data class QueueSettings(
    @Serializable(with = DurationInSecondsSerializer::class)
    @SerialName("waitTimeSeconds")
    val waitTime: Duration = 1.minutes,
    @Serializable(with = DurationInSecondsSerializer::class)
    @SerialName("firstToSolveWaitTimeSeconds")
    val firstToSolveWaitTime: Duration = 2.minutes,
    @Serializable(with = DurationInSecondsSerializer::class)
    @SerialName("featuredRunWaitTimeSeconds")
    val featuredRunWaitTime: Duration = 1.minutes,
    @Serializable(with = DurationInSecondsSerializer::class)
    @SerialName("inProgressRunWaitTimeSeconds")
    val inProgressRunWaitTime: Duration = 5.minutes,
    val maxQueueSize: Int = 20,
    val maxUntestedRun: Int = 15,
)

@Serializable
public sealed class ProblemColorPolicy {
    /**
     * Always show problem colors
     */
    @Serializable
    @SerialName("always")
    public data object Always : ProblemColorPolicy()

    /**
     * Replace problem colors with [colorBeforeStart] until contest start
     */
    @Serializable
    @SerialName("afterStart")
    public data class AfterStart(val colorBeforeStart: Color? = null) : ProblemColorPolicy()

    /**
     * Replace problem colors with [colorBeforeSolved] until a problem is solved
     */
    @Serializable
    @SerialName("whenSolved")
    public data class WhenSolved(val colorBeforeSolved: Color? = null) : ProblemColorPolicy()
}

@Serializable
@OptIn(InefficientContestInfoApi::class)
public data class ContestInfo(
    val name: String,
    val status: ContestStatus,
    val resultType: ContestResultType,
    @SerialName("contestLengthMs")
    @Serializable(with = DurationInMillisecondsSerializer::class)
    val contestLength: Duration,
    @SerialName("freezeTimeMs")
    @Serializable(with = DurationInMillisecondsSerializer::class)
    val freezeTime: Duration?,
    @InefficientContestInfoApi @SerialName("problems") val problemList: List<ProblemInfo>,
    @InefficientContestInfoApi @SerialName("teams") val teamList: List<TeamInfo>,
    @InefficientContestInfoApi @SerialName("groups") val groupList: List<GroupInfo>,
    @InefficientContestInfoApi @SerialName("organizations") val organizationList: List<OrganizationInfo>,
    @InefficientContestInfoApi @SerialName("languages") val languagesList: List<LanguageInfo>,
    val penaltyRoundingMode: PenaltyRoundingMode,
    @Required val emulationSpeed: Double = 1.0,
    @Required val awardsSettings: AwardsSettings = AwardsSettings(),
    @Serializable(with = DurationInSecondsSerializer::class)
    @SerialName("penaltyPerWrongAttemptSeconds")
    @Required val penaltyPerWrongAttempt: Duration = 20.minutes,
    @Required val queueSettings: QueueSettings = QueueSettings(),
    @Required val showTeamsWithoutSubmissions: Boolean = true,
    @Required val problemColorPolicy: ProblemColorPolicy = ProblemColorPolicy.Always,
    @Transient val cdsSupportsFinalization: Boolean = false,
    @Required val customFields: Map<String, String> = emptyMap(),
    @Required @InefficientContestInfoApi @SerialName("persons") val personsList: List<PersonInfo> = emptyList(),
    @Required @InefficientContestInfoApi @SerialName("accounts") val accountsList: List<AccountInfo> = emptyList(),
) {
    public constructor(
        name: String,
        resultType: ContestResultType,
        startTime: Instant,
        contestLength: Duration,
        freezeTime: Duration?,
        problemList: List<ProblemInfo>,
        teamList: List<TeamInfo>,
        groupList: List<GroupInfo>,
        organizationList: List<OrganizationInfo>,
        languagesList: List<LanguageInfo>,
        penaltyRoundingMode: PenaltyRoundingMode,
        penaltyPerWrongAttempt: Duration = 20.minutes,
        cdsSupportsFinalization: Boolean = false,
        customFields: Map<String, String> = emptyMap()
    ) : this(
            name = name,
            status = ContestStatus.byCurrentTime(startTime, freezeTime, contestLength, holdTime = null),
            resultType = resultType, contestLength = contestLength, freezeTime = freezeTime,
            problemList = problemList, teamList = teamList, groupList = groupList, organizationList = organizationList,
            languagesList = languagesList, personsList = emptyList(),
            penaltyRoundingMode = penaltyRoundingMode,
            penaltyPerWrongAttempt = penaltyPerWrongAttempt,
            cdsSupportsFinalization = cdsSupportsFinalization,
            customFields = customFields,
        )

    val groups: Map<GroupId, GroupInfo> by lazy { groupList.associateBy { it.id } }
    val teams: Map<TeamId, TeamInfo> by lazy { teamList.associateBy { it.id } }
    val organizations: Map<OrganizationId, OrganizationInfo> by lazy { organizationList.associateBy { it.id } }
    val problems: Map<ProblemId, ProblemInfo> by lazy { problemList.associateBy { it.id } }
    val languages: Map<LanguageId, LanguageInfo> by lazy { languagesList.associateBy { it.id } }
    val scoreboardProblems: List<ProblemInfo> by lazy { problemList.sortedBy { it.ordinal }.filterNot { it.isHidden } }
    val persons: Map<PersonId, PersonInfo> by lazy { personsList.associateBy { it.id } }
    val accounts: Map<AccountId, AccountInfo> by lazy { accountsList.associateBy { it.id } }
}

public val ContestInfo.startTimeOrZero: Instant
    get() = startTime ?: Instant.fromEpochMilliseconds(0)

public val ContestInfo.startTime: Instant?
    get() = when (status) {
        is ContestStatus.BEFORE -> status.scheduledStartAt
        is ContestStatus.RUNNING -> status.startedAt
        is ContestStatus.OVER -> status.startedAt
        is ContestStatus.FINALIZED -> status.startedAt
    }
public val ContestInfo.currentContestTime: Duration
    get() = when (status) {
        is ContestStatus.BEFORE -> Duration.ZERO
        is ContestStatus.RUNNING -> (Clock.System.now() - status.startedAt) * emulationSpeed
        is ContestStatus.OVER, is ContestStatus.FINALIZED -> contestLength
    }
public fun ContestInfo.instantAt(duration: Duration): Instant {
    return startTimeOrZero + duration / emulationSpeed
}

public fun List<RunInfo>.languages(): List<LanguageInfo> = mapNotNull { it.languageId }.distinct().sortedBy { it.value }.map {
    LanguageInfo(it, it.value, emptyList())
}
