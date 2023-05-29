package org.icpclive.cds.krsu

import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import org.icpclive.api.*
import org.icpclive.cds.ContestParseResult
import org.icpclive.cds.FullReloadContestDataSource
import org.icpclive.cds.common.jsonLoader
import org.icpclive.util.getLogger
import java.util.*
import kotlin.time.Duration
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.seconds

class KRSUDataSource(val properties: Properties) : FullReloadContestDataSource(5.seconds) {

    override suspend fun loadOnce() = parseAndUpdateStandings(
        contestInfoLoader.load(), submissionsLoader.load()
    )

    val teams = mutableMapOf<String, TeamInfo>()
    var lastTeamId: Int = 0

    private fun parseAndUpdateStandings(contest: Contest, submissions: List<Submission>): ContestParseResult {
        val timezoneShift = Duration.parse(properties.getProperty("timezone-shift"))

        val startTime = contest.StartTime - timezoneShift

        val problemsList = contest.ProblemSet.mapIndexed { index, it ->
            ProblemInfo(
                letter = "" + ('A' + index),
                name = "" + ('A' + index),
                id = it.Problem,
                ordinal = index,
                contestSystemId = index.toString(),
            )
        }
//        val problemById = problemsList.associateBy { it.id }

        for (submission in submissions) {
            if (!teams.contains(submission.Login)) {
                teams[submission.Login] =
                    TeamInfo(
                        id = lastTeamId++,
                        name = submission.AuthorName,
                        shortName = submission.AuthorName,
                        contestSystemId = submission.Login,
                        groups = emptyList(),
                        hashTag = null,
                        medias = emptyMap()
                    )
            }
        }
        val contestLength = contest.Length.hours
        val freezeTime = contestLength - 1.hours
        val runs = submissions.map {
            val result = outcomeMap[it.StatusName]
            logger.info("" + (it.ReceivedTime - startTime))
            RunInfo(
                id = it.Id,
                result?.toRunResult(),
                problemId = it.Problem,
                teamId = teams[it.Login]?.id ?: -1,
                percentage = if (result == null) 0.0 else 1.0,
                time = (it.ReceivedTime - timezoneShift) - startTime,
            )
        }.toList()

        val time = Clock.System.now() - startTime
        return ContestParseResult(
            ContestInfo(
                name = "",
                status = when {
                    time < Duration.ZERO -> ContestStatus.BEFORE
                    time < contestLength -> ContestStatus.RUNNING
                    else -> ContestStatus.OVER
                },
                resultType = ContestResultType.ICPC,
                startTime = startTime,
                contestLength = contestLength,
                freezeTime = freezeTime,
                problems = problemsList,
                teams = teams.values.toList(),
                groups = emptyList()
            ),
            runs,
            emptyList()
        )
    }

    private val submissionsLoader = jsonLoader<List<Submission>> { properties.getProperty("submissions-url") }
    private val contestInfoLoader = jsonLoader<Contest> { properties.getProperty("contest-url") }

    companion object {
        private val logger = getLogger(KRSUDataSource::class)
        private val outcomeMap = mapOf(
            "InternalError" to Verdict.Fail,
            "Compile Error" to Verdict.CompilationError,
            "Run-Time Error" to Verdict.RuntimeError,
            "Time Limit Exceeded" to Verdict.TimeLimitExceeded,
            "Memory Limit Exceeded" to Verdict.MemoryLimitExceeded,
            "Output Limit Exceeded" to Verdict.OutputLimitExceeded,
            "Security Violation" to Verdict.SecurityViolation,
            "Wrong Answer" to Verdict.WrongAnswer,
            "Accepted" to Verdict.Accepted,
            "Presentation Error" to Verdict.PresentationError,
        )
    }

    @Serializable
    @Suppress("unused")
    class Submission(
        val Id: Int,
        val Login: String,
        val Problem: Int,
        val Letter: Int,
        val Target: String,
        val Status: Int,
        val StatusName: String,
        val TestPassed: Int,
        @Serializable(with = TimeSerializer::class)
        val ReceivedTime: Instant,
        val AuthorName: String,
    )

    @Serializable
    @Suppress("unused")
    class Contest(
        val Id: Int,
        val ProblemSet: List<Problem>,
        @Serializable(with = TimeSerializer::class)
        val StartTime: Instant,
        val Length: Int
    )

    @Serializable
    @Suppress("unused")
    class Problem(
        val Letter: Int,
        val Problem: Int
    )

    class TimeSerializer : KSerializer<Instant> {
        override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("krsu time", PrimitiveKind.STRING)

        override fun deserialize(decoder: Decoder): Instant {
            return Instant.parse(decoder.decodeString() + "Z")
        }

        override fun serialize(encoder: Encoder, value: Instant) {
            TODO("Not yet implemented")
        }

    }
}

