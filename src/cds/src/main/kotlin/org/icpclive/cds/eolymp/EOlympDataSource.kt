package org.icpclive.cds.eolymp

import com.eolymp.graphql.*
import com.expediagroup.graphql.client.ktor.GraphQLKtorClient
import com.expediagroup.graphql.client.types.GraphQLClientRequest
import kotlinx.datetime.toKotlinInstant
import org.icpclive.api.*
import org.icpclive.cds.common.*
import org.icpclive.cds.settings.EOlympSettings
import org.icpclive.util.Enumerator
import java.net.URL
import java.time.chrono.IsoChronology
import java.time.format.DateTimeFormatterBuilder
import java.time.format.ResolverStyle
import java.time.temporal.ChronoField
import kotlin.time.Duration.Companion.ZERO
import kotlin.time.Duration.Companion.seconds


private suspend fun <T : Any> GraphQLKtorClient.checkedExecute(request: GraphQLClientRequest<T>) = execute(request).let {
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


internal class EOlympDataSource(val settings: EOlympSettings) : FullReloadContestDataSource(5.seconds) {
    val graphQLClient = GraphQLKtorClient(
        URL(settings.url),
        defaultHttpClient(ClientAuth.Bearer(settings.token.value), settings.network)
    )

    fun converStatus(status: String) = ContestStatus.OVER.also { status.let { } }
    fun convertResultType(format: String) = when (format) {
        "ICPC" -> ContestResultType.ICPC
        else -> error("Unknown contest format: $format")
    }

    val dateTimeFormatter = DateTimeFormatterBuilder()
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
        .withChronology(IsoChronology.INSTANCE);

    private val problemIds = Enumerator<String>()
    private val teamIds = Enumerator<String>()
    private val runIds = Enumerator<String>()

    override suspend fun loadOnce(): ContestParseResult {
        val result = graphQLClient.judgeContest(settings.contestId)
        val teams = buildList {
            var cursor: String? = null
            while (true) {
                val x = graphQLClient.teams(
                    settings.contestId,
                    cursor,
                    100
                )
                addAll(x.participants!!.nodes.map {
                    TeamInfo(
                        id = teamIds[it.id],
                        fullName = it.name,
                        displayName = it.name,
                        contestSystemId = it.id,
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
        val contestInfo = ContestInfo(
            name = result.name,
            status = converStatus(result.status),
            resultType = convertResultType(result.format),
            startTime = parseTime(result.startsAt),
            contestLength = result.duration.seconds,
            freezeTime = result.duration.seconds - (result.scoreboard?.freezingTime?.seconds ?: ZERO),
            problemList = result.problems!!.nodes.map {
                ProblemInfo(
                    displayName = ('A'.code + it.index - 1).toChar().toString(),
                    fullName = it.statement?.title ?: "",
                    id = problemIds[it.id],
                    ordinal = it.index,
                    contestSystemId = it.id,
                    maxScore = it.score,
                    scoreMergeMode = ScoreMergeMode.LAST_OK
                )
            },
            teamList = teams,
            groupList = emptyList(),
            organizationList = emptyList(),
            penaltyRoundingMode = PenaltyRoundingMode.EACH_SUBMISSION_UP_TO_MINUTE
        )
        val runs = buildList {
            var cursor: String? = null
            while (true) {
                val x = graphQLClient.submissions(
                    settings.contestId,
                    cursor,
                    100
                )
                addAll(x.submissions!!.nodes.map {
                    RunInfo(
                        id = runIds[it.id],
                        result = when {
                            it.percentage == 1.0 -> Verdict.Accepted
                            else -> Verdict.Rejected
                        }.toRunResult(),
                        percentage = 0.0,
                        problemId = problemIds[it.problem!!.id],
                        teamId = teamIds[it.participant!!.id],
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

    private fun parseTime(s: String) = java.time.Instant.from(dateTimeFormatter.parse(s)).toKotlinInstant()

}