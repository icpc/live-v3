package org.icpclive.cds.plugins.cats

import kotlinx.datetime.*
import kotlinx.datetime.format.DateTimeComponents
import kotlinx.datetime.format.char
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.icpclive.cds.*
import org.icpclive.cds.api.*
import org.icpclive.cds.ktor.DataLoader
import org.icpclive.cds.ktor.KtorNetworkSettingsProvider
import org.icpclive.cds.settings.*
import org.icpclive.cds.util.serializers.FormatterInstantSerializer
import org.icpclive.cds.util.serializers.FormatterLocalDateSerializer
import org.icpclive.ksp.cds.Builder
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Instant

private object ContestTimeSerializer : FormatterLocalDateSerializer(LocalDateTime.Format {
    day()
    char('.')
    monthNumber()
    char('.')
    year()
    char(' ')
    hour()
    char(':')
    minute()
})

private object SubmissionTimeSerializer : FormatterInstantSerializer(DateTimeComponents.Format {
    date(LocalDate.Formats.ISO_BASIC)
    char('T')
    hour()
    minute()
    second()
    offset(UtcOffset.Formats.ISO_BASIC)
})

@Builder("cats")
public sealed interface CatsSettings : CDSSettings, KtorNetworkSettingsProvider {
    public val login: Credential
    public val password: Credential
    public val source: UrlOrLocalPath.Url
    public val cid: String
    public val timeZone: TimeZone
        get() = TimeZone.of("Asia/Vladivostok")
    public val resultType: ContestResultType
        get() = ContestResultType.ICPC

    override fun toDataSource(): ContestDataSource = CATSDataSource(this)
}

internal class CATSDataSource(val settings: CatsSettings) : FullReloadContestDataSource(5.seconds) {
    private val login = settings.login.value
    private val password = settings.password.value

    private var sid: String? = null

    @Serializable
    data class Auth(val status: String, val sid: String, val cid: Long)

    @Serializable
    data class Problem(val id: Int, val name: String, val code: String, val max_points: Double = 0.0)

    @Serializable
    data class Problems(val problems: List<Problem>)

    @Serializable
    data class Team(val id: Int, val account_id: Int, val login: String, val name: String, val role: String)

    @Serializable
    data class Users(val users: List<Team>)

    @Serializable
    data class Contest(
        val title: String,
        @Serializable(with = ContestTimeSerializer::class)
        val start_date: LocalDateTime,
        @Serializable(with = ContestTimeSerializer::class)
        val freeze_date: LocalDateTime,
        @Serializable(with = ContestTimeSerializer::class)
        val finish_date: LocalDateTime,
        val rules: String,
    )

    @Serializable
    sealed class Run

    @Serializable
    @SerialName("submit")
    data class Submit(
        val id: Int,
        val state_text: String,
        val problem_id: Int,
        val team_id: Int,
        @Serializable(with = SubmissionTimeSerializer::class)
        val submit_time: Instant,
        val points: Double = 0.0,
    ) : Run()

    @Serializable
    @SerialName("broadcast")
    @Suppress("unused")
    data class Broadcast(
        val text: String,
    ) : Run()

    // NOTICE: May it
    @Serializable
    @SerialName("c.question")
    @Suppress("unused")
    data class Question(
        val text: String,
    ) : Run()

    @Serializable
    @SerialName("contest")
    @Suppress("unused")
    data class ContestStart(
        val contest_start: Int,
    ) : Run()

    private val authLoader = DataLoader.json<Auth>(networkSettings = settings.network) { settings.source.subDir("?f=login&login=$login&passwd=$password&json=1") }
    private val problemsLoader = DataLoader.json<Problems>(networkSettings = settings.network) { settings.source.subDir("problems?cid=${settings.cid}&sid=${sid!!}&rows=1000&json=1") }
    private val usersLoader = DataLoader.json<Users>(networkSettings = settings.network) { settings.source.subDir("users?cid=${settings.cid}&sid=${sid!!}&rows=1000&json=1") }
    private val contestLoader = DataLoader.json<Contest>(networkSettings = settings.network) { settings.source.subDir("contest_params?cid=${settings.cid}&sid=${sid!!}&json=1") }
    private val runsLoader = DataLoader.json<List<Run>>(networkSettings = settings.network) {
        settings.source.subDir("console?cid=${settings.cid}&sid=${sid!!}&rows=1000&json=1&search=is_ooc%3D0&show_messages=0&show_contests=0&show_results=1")
    }

    override suspend fun loadOnce(): ContestParseResult {
        sid = authLoader.load().sid
        return parseAndUpdateStandings(
            problemsLoader.load(),
            usersLoader.load(),
            contestLoader.load(),
            runsLoader.load()
        )
    }

    private fun parseAndUpdateStandings(
        problems: Problems,
        users: Users,
        contest: Contest,
        runs: List<Run>,
    ): ContestParseResult {
        val problemsList: List<ProblemInfo> = problems.problems
            .asSequence()
            .mapIndexed { index, problem ->
                ProblemInfo(
                    id = problem.id.toProblemId(),
                    displayName = problem.code,
                    fullName = problem.name,
                    ordinal = index,
                    minScore = if (settings.resultType == ContestResultType.IOI) 0.0 else null,
                    maxScore = if (settings.resultType == ContestResultType.IOI) problem.max_points else null,
                    color = null,
                    scoreMergeMode = if (settings.resultType == ContestResultType.IOI) ScoreMergeMode.MAX_TOTAL else null
                )
            }
            .toList()

        val teamList: List<TeamInfo> = users.users
            .asSequence()
            .filter { team -> team.role == "in_contest" }
            .map { team ->
                TeamInfo(
                    id = team.account_id.toTeamId(),
                    fullName = team.name,
                    displayName = team.name,
                    groups = emptyList(),
                    hashTag = null,
                    medias = mapOf(),
                    isHidden = false,
                    isOutOfContest = false,
                    organizationId = null
                )
            }.toList()

        val startTime = contest.start_date.toInstant(settings.timeZone)
        val contestLength = contest.finish_date.toInstant(settings.timeZone) - startTime
        val freezeTime = contest.freeze_date.toInstant(settings.timeZone) - startTime

        val resultRuns = runs
            .asSequence()
            .filterIsInstance<Submit>()
            .map {
                val result = if (it.state_text.isNotEmpty()) {
                    when (settings.resultType) {
                        ContestResultType.ICPC -> Verdict.lookup(
                            shortName = it.state_text,
                            isAccepted = ("OK" == it.state_text),
                            isAddingPenalty = ("OK" != it.state_text && "CE" != it.state_text),
                        ).toICPCRunResult()

                        ContestResultType.IOI -> RunResult.IOI(score = listOf(it.points))
                    }
                } else RunResult.InProgress(0.0)
                RunInfo(
                    id = it.id.toRunId(),
                    result = result,
                    problemId = it.problem_id.toProblemId(),
                    teamId = it.team_id.toTeamId(),
                    time = it.submit_time - startTime,
                    languageId = null
                )
            }
            .toList()
            .sortedBy { it.time }

        val contestInfo = ContestInfo(
            name = contest.title,
            resultType = settings.resultType,
            startTime = startTime,
            contestLength = contestLength,
            freezeTime = freezeTime,
            problemList = problemsList,
            teamList = teamList,
            groupList = emptyList(),
            organizationList = emptyList(),
            penaltyRoundingMode = when (settings.resultType) {
                ContestResultType.IOI -> PenaltyRoundingMode.ZERO
                ContestResultType.ICPC -> PenaltyRoundingMode.EACH_SUBMISSION_DOWN_TO_MINUTE
            },
            languagesList = resultRuns.languages(),
        )


        return ContestParseResult(
            contestInfo,
            resultRuns,
            emptyList()
        )
    }
}