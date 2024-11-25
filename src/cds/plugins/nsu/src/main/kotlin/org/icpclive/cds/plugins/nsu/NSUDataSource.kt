package org.icpclive.cds.plugins.nsu


import io.ktor.client.call.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.cookies.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.datetime.*
import kotlinx.datetime.format.char
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.*
import org.icpclive.cds.*
import org.icpclive.cds.api.*
import org.icpclive.ksp.cds.Builder
import org.icpclive.cds.ktor.*
import org.icpclive.cds.settings.*
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds


@Builder("nsu")
public sealed interface NSUSettings : CDSSettings, KtorNetworkSettingsProvider {
    public val source: UrlOrLocalPath.Url
    public val olympiadId: Int
    public val tourId: Int
    public val email: Credential
    public val password: Credential
    public val timeZone: TimeZone
        get() = TimeZone.of("Asia/Novosibirsk")

    override fun toDataSource(): ContestDataSource = NSUDataSource(this)
}

internal class NSUDataSource(val settings: NSUSettings) : FullReloadContestDataSource(5.seconds) {

    val format = Json { ignoreUnknownKeys = true }

    @Serializable
    class Credentials(
        val email: String,
        val password: String,
        val method: String = "internal",
    )

    @Serializable
    class Team(
        val id: Int,
        val title: String,
    )

    @Serializable
    class Task(
        val id: Int,
        val title: String,
    )

    @Serializable
    class TourTimes(
        val tour_start_time: String,
        val tour_end_time: String,
        val rating_freeze_time: String?,
    )

    override suspend fun loadOnce(): ContestParseResult {
        val queueLimit = 999999
        val loginUrl = "${settings.source}/api/login"
        val selectOlympiadUrl = "${settings.source}/api/olympiads/enter"
        val selectTourUrl = "${settings.source}/api/tours/enter?tour=${settings.tourId}"
        val submissionsUrl = "${settings.source}/api/queue/submissions?limit=" + queueLimit.toString()
        val filtersUrl = "${settings.source}/api/queue/filters"
        val ratingUrl = "${settings.source}/api/rating/rating?showFrozen=0"


        httpClient.post(loginUrl) {
            contentType(ContentType.Application.Json)
            setBody(Credentials(settings.email.value, settings.password.value))
        }.bodyAsText()

        httpClient.post(selectOlympiadUrl) {
            contentType(ContentType.Application.Json)
            setBody(mapOf("olympiad" to settings.olympiadId.toString()))
        }.bodyAsText()

        httpClient.get(selectTourUrl).bodyAsText()

        val queue: JsonObject = httpClient.get(submissionsUrl) {
            contentType(ContentType.Application.Json)
        }.body()


        val submissions: List<Submission> = format.decodeFromJsonElement(queue["submissions"] as JsonArray)
        val filters: JsonObject = httpClient.get(filtersUrl).body()
        val teams: List<Team> = format.decodeFromJsonElement(filters["teams"] as JsonArray)
        val tasks: List<Task> = format.decodeFromJsonElement(filters["tasks"] as JsonArray)

        val teamList: List<TeamInfo> = teams.map {
            TeamInfo(
                id = it.id.toTeamId(),
                fullName = it.title,
                displayName = it.title,
                groups = emptyList(),
                hashTag = null,
                medias = emptyMap(),
                isHidden = false,
                isOutOfContest = false,
                organizationId = null,
            )
        }.sortedBy { it.id.value }


        val problemsList: List<ProblemInfo> = tasks.mapIndexed { index, it ->
            ProblemInfo(
                id = it.id.toProblemId(),
                displayName = it.title.substringBefore('.'),
                fullName = it.title,
                ordinal = index
            )
        }

        val rating: JsonObject = httpClient.post(ratingUrl) {
            contentType(ContentType.Application.Json)
            setBody(mapOf("selectedAttributes" to emptyList<String>().toString()))
        }.body()

        val timeSettings: TourTimes = format.decodeFromJsonElement(rating["tourTimes"] as JsonObject)
        val contestName: String = rating["tourTitle"]?.jsonPrimitive.toString()


        val startTime = parseNSUTime(timeSettings.tour_start_time)
        val contestLength = parseNSUTime(timeSettings.tour_end_time) - startTime

        val freezeTime: Duration = if (timeSettings.rating_freeze_time != null) {
            parseNSUTime(timeSettings.rating_freeze_time) - startTime
        } else {
            contestLength
        }

        val runs: List<RunInfo> = submissions.map {
            RunInfo(
                id = it.id.toRunId(),
                result = getRunResult(it.res, it.status) ?: RunResult.InProgress(0.0),
                problemId = it.taskId.toProblemId(),
                teamId = it.teamId.toTeamId(),
                time = parseNSUTime(it.smtime) - startTime,
                languageId = null
            )
        }

        val info = ContestInfo(
            name = contestName,
            resultType = ContestResultType.ICPC,
            startTime = startTime,
            contestLength = contestLength,
            freezeTime = freezeTime,
            problemList = problemsList,
            teamList = teamList,
            groupList = emptyList(),
            organizationList = emptyList(),
            languagesList = runs.languages(),
            penaltyRoundingMode = PenaltyRoundingMode.EACH_SUBMISSION_DOWN_TO_MINUTE,
        )

        return ContestParseResult(info, runs, emptyList())

    }

    private val httpClient = settings.network.createHttpClient {
        install(HttpCookies)
        install(ContentNegotiation) { json() }
    }

    private fun getRunResult(result: String?, status: Int): RunResult? {
        val verdict = when (val letter = result?.last()) {
            'A' -> Verdict.Accepted
            'C' -> Verdict.CompilationError
            // "Deadlock - Timeout": astronomical time limit exceeded
            'D' -> Verdict.TimeLimitExceeded
            'M' -> Verdict.MemoryLimitExceeded
            // "No output file"
            'O' -> Verdict.PresentationError
            'P' -> Verdict.PresentationError
            'S' -> Verdict.SecurityViolation
            'R' -> Verdict.RuntimeError
            'T' -> Verdict.TimeLimitExceeded
            'W' -> Verdict.WrongAnswer
            // "Static Analysis Failed"
            'X' -> Verdict.CompilationError
            // "Dynamic Analysis failed"
            'Y' -> Verdict.CompilationErrorWithPenalty
            '.' -> Verdict.Ignored
            'F', 'J', 'K' -> null
            else -> error("Unknown verdict: $letter")
        }
        if (verdict == Verdict.Accepted && status != 3) return null
        return verdict?.toICPCRunResult()
    }

    private val timePattern = LocalDateTime.Format {
        date(LocalDate.Formats.ISO)
        char(' ')
        time(LocalTime.Formats.ISO)
    }

    private fun parseNSUTime(time: String): Instant {
        return timePattern.parse(time).toInstant(settings.timeZone)
    }

    @Serializable
    class Submission(
        val id: Int,
        val teamId: Int,
        val smtime: String,
        val status: Int,
        val res: String?,
        val taskId: Int,
    )
}


