package org.icpclive.cds.plugins.eolymp

import com.eolymp.graphql.*
import com.expediagroup.graphql.client.ktor.GraphQLKtorClient
import com.expediagroup.graphql.client.types.GraphQLClientRequest
import kotlinx.datetime.toKotlinInstant
import org.icpclive.cds.*
import org.icpclive.cds.api.*
import org.icpclive.cds.ksp.Builder
import org.icpclive.cds.ktor.*
import org.icpclive.cds.settings.CDSSettings
import org.icpclive.cds.settings.Credential
import org.icpclive.util.Enumerator
import org.icpclive.util.getLogger
import java.net.URL
import java.time.chrono.IsoChronology
import java.time.format.DateTimeFormatterBuilder
import java.time.format.ResolverStyle
import java.time.temporal.ChronoField
import kotlin.time.Duration.Companion.ZERO
import kotlin.time.Duration.Companion.seconds


private suspend fun <T : Any> GraphQLKtorClient.checkedExecute(request: GraphQLClientRequest<T>) =
    execute(request).let {
        if (it.errors.isNullOrEmpty()) {
            it.data!!
        } else {
            throw IllegalStateException("Error in graphql query: ${it.errors}")
        }
    }

private suspend fun GraphQLKtorClient.judgeContest(id: String) = checkedExecute(
    JudgeContestDetails(
        JudgeContestDetails.Variables(
            locale = "en",
            id = id
        )
    )
).contest

private suspend fun GraphQLKtorClient.submissions(contestId: String, after: String?, count: Int) = checkedExecute(
    JudgeContestSubmissions(
        JudgeContestSubmissions.Variables(
            id = contestId,
            after = after,
            count = count
        )
    )
).contest


private suspend fun GraphQLKtorClient.teams(contestId: String, after: String?, count: Int) = checkedExecute(
    JudgeContestTeams(
        JudgeContestTeams.Variables(
            id = contestId,
            after = after,
            count = count
        )
    )
).contest


@Builder("eolymp")
public sealed interface EOlympSettings : CDSSettings {
    public val url: String
    public val token: Credential
    public val contestId: String
    public val previousDaysContestIds: List<String>
        get() = emptyList()

    override fun toDataSource(): ContestDataSource = EOlympDataSource(this)
}

internal class EOlympDataSource(val settings: EOlympSettings) : FullReloadContestDataSource(5.seconds) {
    private val graphQLClient = GraphQLKtorClient(
        URL(settings.url),
        defaultHttpClient(ClientAuth.Bearer(settings.token.value), settings.network)
    )

    private fun convertStatus(status: String) = when (status) {
        "STATUS_UNKNOWN" -> error("Doc said $status should not be used")
        "SCHEDULED" -> ContestStatus.BEFORE
        "OPEN", "SUSPENDED", "FROZEN" -> ContestStatus.RUNNING
        "COMPLETE" -> ContestStatus.OVER
        else -> error("Unknown status: $status")
    }

    private fun convertResultType(format: String) = when (format) {
        "ICPC" -> ContestResultType.ICPC
        "IOI" -> ContestResultType.IOI
        else -> error("Unknown contest format: $format")
    }

    private val dateTimeFormatter = DateTimeFormatterBuilder()
        .parseCaseInsensitive()
        .appendValue(ChronoField.YEAR, 4)
        .appendLiteral('-')
        .appendValue(ChronoField.MONTH_OF_YEAR, 2)
        .appendLiteral('-')
        .appendValue(ChronoField.DAY_OF_MONTH, 2)
        .appendLiteral('T')
        .appendValue(ChronoField.HOUR_OF_DAY, 2)
        .appendLiteral(':')
        .appendValue(ChronoField.MINUTE_OF_HOUR, 2)
        .appendLiteral(':')
        .appendValue(ChronoField.SECOND_OF_MINUTE, 2)
        .optionalStart()
        .appendFraction(ChronoField.NANO_OF_SECOND, 2, 9, true)
        .optionalEnd()
        .appendOffset("+HH:MM", "Z")
        .toFormatter()
        .withResolverStyle(ResolverStyle.STRICT)
        .withChronology(IsoChronology.INSTANCE)

    private val runIds = Enumerator<String>()

    private var previousDays: List<ContestParseResult> = emptyList()

    @OptIn(InefficientContestInfoApi::class)
    override suspend fun loadOnce(): ContestParseResult {
        if (settings.previousDaysContestIds.isEmpty()) {
            return loadContest(settings.contestId)
        }
        if (previousDays.isEmpty())
            previousDays = settings.previousDaysContestIds.map { loadContest(it) }
        val lastDay = loadContest(settings.contestId)
        val teamIdToMemberIdMap =
            (previousDays.flatMap { it.contestInfo.teamList } + lastDay.contestInfo.teamList).associate {
                it.id to it.fullName
            }
        val memberIdToTeamIdMap = lastDay.contestInfo.teamList.associate {
            it.fullName to it.id
        }
        val problemList = (previousDays + lastDay).flatMap {
            it.contestInfo.problemList.sortedBy { it.ordinal }
        }.mapIndexed { index, problemInfo ->
            problemInfo.copy(ordinal = index)
        }

        return ContestParseResult(
            lastDay.contestInfo.copy(
                problemList = problemList
            ),
            previousDays.flatMap {
                it.runs.mapNotNull {
                    it.copy(
                        time = ZERO,
                        teamId = memberIdToTeamIdMap[teamIdToMemberIdMap[it.teamId]!!] ?: return@mapNotNull null
                    )
                }
            } + lastDay.runs,
            emptyList()
        )
    }

    private suspend fun loadContest(contestId: String): ContestParseResult {
        val result = graphQLClient.judgeContest(contestId)
        val teams = buildList {
            var cursor: String? = null
            while (true) {
                val x = graphQLClient.teams(
                    contestId,
                    cursor,
                    100
                )
                addAll(x.participants!!.nodes.map {
                    TeamInfo(
                        id = TeamId(it.id),
                        fullName = it.name,
                        displayName = it.name,
                        groups = emptyList(),
                        hashTag = null,
                        medias = emptyMap(),
                        isHidden = false,
                        isOutOfContest = it.unofficial,
                        organizationId = null
                    )
                })
                if (!x.participants.pageInfo.hasNextPage) break
                cursor = x.participants.pageInfo.endCursor
            }
        }
        val resultType = convertResultType(result.format)
        val contestInfo = ContestInfo(
            name = result.name,
            status = convertStatus(result.status),
            resultType = resultType,
            startTime = parseTime(result.startsAt),
            contestLength = result.duration.seconds,
            freezeTime = result.duration.seconds - (result.scoreboard?.freezingTime?.seconds ?: ZERO),
            problemList = result.problems!!.nodes.map {
                ProblemInfo(
                    id = ProblemId(it.id),
                    displayName = ('A'.code + it.index - 1).toChar().toString(),
                    fullName = it.statement?.title ?: "",
                    ordinal = it.index,
                    minScore = if (resultType == ContestResultType.IOI) 0.0 else null,
                    maxScore = if (resultType == ContestResultType.IOI) it.score else null,
                    scoreMergeMode = if (resultType == ContestResultType.IOI) {
                        if (it.scoreByBestTestset) ScoreMergeMode.MAX_PER_GROUP else ScoreMergeMode.MAX_TOTAL
                    } else null
                )
            },
            teamList = teams,
            groupList = emptyList(),
            organizationList = emptyList(),
            penaltyRoundingMode = when (resultType) {
                ContestResultType.ICPC -> PenaltyRoundingMode.EACH_SUBMISSION_UP_TO_MINUTE
                ContestResultType.IOI -> PenaltyRoundingMode.ZERO
            }
        )
        val runs = buildList {
            var cursor: String? = null
            while (true) {
                val x = graphQLClient.submissions(
                    contestId,
                    cursor,
                    100
                )
                addAll(x.submissions!!.nodes.map {
                    val verdict = parseVerdict(it.status, it.verdict, it.percentage)
                    RunInfo(
                        id = runIds[it.id],
                        result = when (resultType) {
                            ContestResultType.ICPC -> verdict?.toICPCRunResult()
                            ContestResultType.IOI -> RunResult.IOI(it.groups.map { it.score }).takeIf { verdict != null }
                        } ?: RunResult.InProgress(0.0),
                        problemId = ProblemId(it.problem!!.id),
                        teamId = TeamId(it.participant!!.id),
                        time = parseTime(it.submittedAt) - contestInfo.startTime,
                        isHidden = it.deleted
                    )
                })
                if (!x.submissions.pageInfo.hasNextPage) break
                cursor = x.submissions.pageInfo.endCursor
            }
        }
        return ContestParseResult(
            contestInfo,
            runs,
            emptyList()
        )
    }

    private fun parseVerdict(status: String, verdict: String, percentage: Double): Verdict? {
        return when (status) {
            "ERROR" -> Verdict.CompilationError
            "PENDING", "TESTING" -> null
            "COMPLETE" -> {
                when (verdict) {
                    "NO_VERDICT" -> when {
                        percentage == 1.0 -> Verdict.Accepted
                        else -> Verdict.Rejected
                    }

                    "ACCEPTED" -> Verdict.Accepted
                    "WRONG_ANSWER" -> Verdict.WrongAnswer
                    "TIME_LIMIT_EXCEEDED" -> Verdict.IdlenessLimitExceeded
                    "CPU_EXHAUSTED" -> Verdict.TimeLimitExceeded
                    "MEMORY_OVERFLOW" -> Verdict.MemoryLimitExceeded
                    "RUNTIME_ERROR" -> Verdict.RuntimeError
                    else -> {
                        log.info("Unknown verdict: $verdict, assuming rejected")
                        Verdict.Rejected
                    }
                }
            }

            else -> {
                log.info("Unexpected submission status: $status, assuming untested")
                null
            }
        }
    }

    private fun parseTime(s: String) = java.time.Instant.from(dateTimeFormatter.parse(s)).toKotlinInstant()

    companion object {
        val log = getLogger(EOlympDataSource::class)
    }
}