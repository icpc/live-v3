package org.icpclive.cds.cats

import kotlinx.datetime.*
import kotlinx.serialization.*
import kotlinx.serialization.descriptors.*
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import org.icpclive.api.*
import org.icpclive.cds.common.*
import org.icpclive.cds.settings.CatsSettings
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import kotlin.time.Duration.Companion.seconds

private object ContestTimeSerializer : KSerializer<LocalDateTime> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("InstantConstand", PrimitiveKind.STRING)
    private val formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm")

    override fun serialize(encoder: Encoder, value: LocalDateTime) {
        encoder.encodeString(formatter.format(value.toJavaLocalDateTime()))
    }

    override fun deserialize(decoder: Decoder): LocalDateTime {
        return java.time.LocalDateTime.parse(decoder.decodeString(), formatter).toKotlinLocalDateTime()
    }
}

private object SubmissionTimeSerializer : KSerializer<Instant> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("InstantConstand", PrimitiveKind.STRING)
    private val formatter = DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmssZ")

    override fun serialize(encoder: Encoder, value: Instant) {
        encoder.encodeString(formatter.format(value.toJavaInstant()))
    }

    override fun deserialize(decoder: Decoder): Instant {
        return ZonedDateTime.parse(decoder.decodeString(), formatter).toInstant().toKotlinInstant()
    }
}

internal class CATSDataSource(val settings: CatsSettings) : FullReloadContestDataSource(5.seconds) {
    private val login = settings.login.value
    private val password = settings.password.value

    private var sid: String? = null

//    variables for parsing runs
    private var page = 0
    private val MAX_PAGE = 100

    @Serializable
    data class Auth(val status: String, val sid: String, val cid: Long)

    @Serializable
    data class Problem(val id: Int, val name: String, val code: String, val max_points: String = "0.0")

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
        val rules: String
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
        val points: Double = 0.0
    ) : Run()

    @Serializable
    @SerialName("broadcast")
    @Suppress("unused")
    data class Broadcast(
        val text: String
    ) : Run()

    // NOTICE: May it
    @Serializable
    @SerialName("c.question")
    @Suppress("unused")
    data class Question(
        val text: String
    ) : Run()

    @Serializable
    @SerialName("contest")
    @Suppress("unused")
    data class ContestStart(
        val contest_start: Int
    ) : Run()

    private val authLoader = jsonLoader<Auth>(networkSettings = settings.network) { "${settings.url}/?f=login&login=$login&passwd=$password&json=1" }
    private val problemsLoader = jsonLoader<Problems>(networkSettings = settings.network) { "${settings.url}/problems?cid=${settings.cid}&sid=${sid!!}&rows=1000&json=1" }
    private val usersLoader = jsonLoader<Users>(networkSettings = settings.network) { "${settings.url}/users?cid=${settings.cid}&sid=${sid!!}&rows=1000&json=1" }
    private val contestLoader = jsonLoader<Contest>(networkSettings = settings.network) { "${settings.url}/contest_params?cid=${settings.cid}&sid=${sid!!}&json=1" }
    private val runsLoader = jsonLoader<List<Run>>(networkSettings = settings.network) { "${settings.url}/console?cid=${settings.cid}&sid=${sid!!}&rows=1000&json=1&search=is_ooc%3D0&show_messages=0&show_contests=0&show_results=1&page=$page" }

    override suspend fun loadOnce(): ContestParseResult {
        sid = authLoader.load().sid
        return parseAndUpdateStandings(
            problemsLoader.load(),
            usersLoader.load(),
            contestLoader.load(),
            parseSubmitPages()
        )
    }

    private suspend fun parseSubmitPages(): List<Run> {
        val runs = mutableSetOf<Int>()
        val result = mutableListOf<Run>()
        for (currentPage in 0 until MAX_PAGE) {
            page = currentPage
            val pageSubmits = runsLoader.load().filterIsInstance<Submit>()

            if (runs.intersect(pageSubmits.map(Submit::id).toSet()).size == pageSubmits.size) {
                break
            } else {
                addNewSubmits(runs, result, pageSubmits)
            }
        }
        return result
    }

    private fun addNewSubmits(
        runs: MutableSet<Int>,
        result: MutableList<Run>,
        pageSubmits: List<Submit>
    ) {
        val newSubmits = pageSubmits.filter { it.id !in runs }
        result.addAll(newSubmits)
        runs.addAll(newSubmits.map(Submit::id))
    }


    private fun parseAndUpdateStandings(
        problems: Problems,
        users: Users,
        contest: Contest,
        runs: List<Run>
    ): ContestParseResult {
        val problemsList: List<ProblemInfo> = problems.problems
            .asSequence()
            .mapIndexed { index, problem ->
                ProblemInfo(
                    displayName = problem.code,
                    fullName = problem.name,
                    color = null,
                    id = problem.id,
                    ordinal = index,
                    contestSystemId = problem.id.toString(),
                    minScore = if (settings.resultType == ContestResultType.IOI) 0.0 else null,
                    maxScore = if (settings.resultType == ContestResultType.IOI) problem.max_points.toDouble() else null,
                    scoreMergeMode = if (settings.resultType == ContestResultType.IOI) ScoreMergeMode.MAX_TOTAL else null
                )
            }
            .toList()

        val teamList: List<TeamInfo> = users.users
            .asSequence()
            .filter { team -> team.role == "in_contest" }
            .map { team ->
                TeamInfo(
                    id = team.account_id,
                    fullName = team.name,
                    displayName = team.name,
                    contestSystemId = team.account_id.toString(),
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

        val contestInfo = ContestInfo(
            name = contest.title,
            status = ContestStatus.byCurrentTime(startTime, contestLength),
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
            }
        )

        val resultRuns = runs
            .asSequence()
            .filterIsInstance<Submit>()
            .map {
                val result = if (it.state_text.isNotEmpty()) {
                    when (contestInfo.resultType) {
                        ContestResultType.ICPC -> Verdict.lookup(
                                shortName = it.state_text,
                                isAccepted = ("OK" == it.state_text),
                                isAddingPenalty = ("OK" != it.state_text && "CE" != it.state_text),
                            ).toRunResult()
                        ContestResultType.IOI -> IOIRunResult(score = listOf(it.points))
                    }
                } else null
                RunInfo(
                    id = it.id,
                    result = result,
                    problemId = it.problem_id,
                    teamId = it.team_id,
                    percentage = if (result == null) 0.0 else 1.0,
                    time = it.submit_time - startTime
                )
            }
            .toList()
            .sortedBy { it.time }

        return ContestParseResult(
            contestInfo,
            resultRuns,
            emptyList()
        )
    }
}