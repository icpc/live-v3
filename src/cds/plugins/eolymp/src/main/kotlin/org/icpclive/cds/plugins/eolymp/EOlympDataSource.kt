package org.icpclive.cds.plugins.eolymp

import com.eolymp.graphql.*
import com.expediagroup.graphql.client.ktor.GraphQLKtorClient
import com.expediagroup.graphql.client.types.GraphQLClientRequest
import kotlinx.datetime.Instant
import kotlinx.datetime.format.DateTimeComponents
import org.icpclive.cds.*
import org.icpclive.cds.api.*
import org.icpclive.ksp.cds.Builder
import org.icpclive.cds.ktor.*
import org.icpclive.cds.settings.*
import org.icpclive.cds.util.getLogger
import java.net.URL
import kotlin.time.Duration
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

private suspend fun GraphQLKtorClient.submissions(contestId: String, offset: Int?, count: Int) = checkedExecute(
    JudgeContestSubmissions(
        JudgeContestSubmissions.Variables(
            id = contestId,
            offset = offset,
            count = count
        )
    )
).contest


private suspend fun GraphQLKtorClient.teams(contestId: String, offset: Int?, count: Int) = checkedExecute(
    JudgeContestTeams(
        JudgeContestTeams.Variables(
            id = contestId,
            offset = offset,
            count = count
        )
    )
).contest


@Builder("eolymp")
public sealed interface EOlympSettings : CDSSettings, KtorNetworkSettingsProvider {
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
        settings.network.createHttpClient {
            setupAuth(Authorization().withBearer(settings.token))
        }
    )

    private fun convertStatus(status: String, startTime: Instant, contestLength: Duration, freezeTime: Duration?) = when (status) {
        "STATUS_UNKNOWN" -> error("Doc said $status should not be used")
        "SCHEDULED" -> ContestStatus.BEFORE(scheduledStartAt = startTime)
        "OPEN", "SUSPENDED", "FROZEN" -> ContestStatus.RUNNING(startedAt = startTime, frozenAt = freezeTime?.let {  startTime + it }.takeIf { status == "FROZEN" })
        "COMPLETE" -> ContestStatus.OVER(startedAt = startTime, finishedAt = startTime + contestLength, frozenAt = freezeTime?.let {  startTime + it })
        else -> error("Unknown status: $status")
    }

    private fun convertResultType(format: String) = when (format) {
        "ICPC" -> ContestResultType.ICPC
        "IOI" -> ContestResultType.IOI
        else -> error("Unknown contest format: $format")
    }

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
            var cursor: Int = 0
            while (true) {
                val x = graphQLClient.teams(
                    contestId,
                    cursor,
                    100
                )
                addAll(x.participants!!.nodes.map {
                    TeamInfo(
                        id = it.id.toTeamId(),
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
                if (x.participants!!.nodes.size < 100) break
                cursor += 100
            }
        }
        val resultType = convertResultType(result.format)
        val startTime = parseTime(result.startsAt)
        val contestLength = result.duration.seconds
        val freezeTime = result.scoreboard?.freezingTime?.seconds?.let { contestLength - it }
        val runs = buildList {
            var cursor: Int = 0
            while (true) {
                val x = graphQLClient.submissions(
                    contestId,
                    cursor,
                    100
                )
                addAll(x.submissions!!.nodes.map {
                    val verdict = parseVerdict(it.status, it.verdict, it.percentage)
                    RunInfo(
                        id = it.id.toRunId(),
                        result = if (verdict == null) {
                            RunResult.InProgress(0.0)
                        } else {
                            when (resultType) {
                                ContestResultType.ICPC -> verdict.toICPCRunResult()
                                ContestResultType.IOI -> RunResult.IOI(it.groups.map { it.score }.ifEmpty { listOf(it.score) })
                            }
                        },
                        problemId = it.problem!!.id.toProblemId(),
                        teamId = it.participant!!.id.toTeamId(),
                        time = parseTime(it.submittedAt) - startTime,
                        isHidden = it.deleted,
                        languageId = it.lang.toLanguageId()
                    )
                })
                if (x.submissions.nodes.size < 100) break
                cursor += 100
            }
        }
        val contestInfo = ContestInfo(
            name = result.name,
            status = convertStatus(result.status, startTime, contestLength, freezeTime),
            resultType = resultType,
            contestLength = contestLength,
            freezeTime = freezeTime,
            problemList = result.problems!!.nodes.map {
                ProblemInfo(
                    id = it.id.toProblemId(),
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
            languagesList = runs.languages(),
            penaltyRoundingMode = when (resultType) {
                ContestResultType.ICPC -> PenaltyRoundingMode.EACH_SUBMISSION_UP_TO_MINUTE
                ContestResultType.IOI -> PenaltyRoundingMode.ZERO
            }
        )
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
                        log.info { "Unknown verdict: $verdict, assuming rejected" }
                        Verdict.Rejected
                    }
                }
            }

            else -> {
                log.info { "Unexpected submission status: $status, assuming untested" }
                null
            }
        }
    }

    private fun parseTime(s: String) = Instant.parse(s, DateTimeComponents.Formats.ISO_DATE_TIME_OFFSET)

    companion object {
        val log by getLogger()
    }
}