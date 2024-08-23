package org.icpclive.cds.plugins.testsys

import kotlinx.datetime.*
import kotlinx.datetime.format.*
import org.icpclive.cds.*
import org.icpclive.cds.api.*
import org.icpclive.ksp.cds.Builder
import org.icpclive.cds.ktor.*
import org.icpclive.cds.settings.CDSSettings
import org.icpclive.cds.settings.UrlOrLocalPath
import java.nio.charset.Charset
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

@Builder("testsys")
public sealed interface TestSysSettings : CDSSettings {
    public val source: UrlOrLocalPath
    public val timeZone: TimeZone
        get() = TimeZone.of("Europe/Moscow")

    override fun toDataSource(): ContestDataSource = TestSysDataSource(this)
}

internal class TestSysDataSource(val settings: TestSysSettings) : FullReloadContestDataSource(5.seconds) {
    private val loader = DataLoader.byteArray(settings.network, settings.source)
        .map {
            val eofPosition = it.indexOf(EOF)
            String(
                it,
                eofPosition + 1, it.size - eofPosition - 1,
                Charset.forName("windows-1251")
            )
        }.map {
            it.split("\r\n", "\n").filter(String::isNotEmpty)
        }

    override suspend fun loadOnce(): ContestParseResult {
        val data = loader.load().groupBy(
            keySelector = { it.split(" ", limit = 2)[0] },
            valueTransform = { it.split(" ", limit = 2)[1] }
        )
        val problemsWithPenalty = (data["@p"] ?: emptyList()).mapIndexed { index, prob ->
            val (letter, name, penalty) = prob.splitCommas()
            ProblemInfo(
                id = letter.toProblemId(),
                displayName = letter,
                fullName = name,
                ordinal = index,
            ) to penalty.toInt()
        }
        val penalty = problemsWithPenalty.map { it.second }.distinct().takeIf { it.size <= 1 }
            ?: TODO("Different problem penalties are not supported")
        val teams = (data["@t"] ?: emptyList()).mapIndexed { index, team ->
            val (id, _, _, name) = team.splitCommas()
            TeamInfo(
                id = id.toTeamId(),
                fullName = name,
                displayName = name,
                groups = emptyList(),
                hashTag = null,
                medias = emptyMap(),
                isOutOfContest = false,
                isHidden = false,
                organizationId = null
            )
        }
        val isCEPenalty = data["@comment"]?.contains("@pragma IgnoreCE") != true
        val problems = problemsWithPenalty.map { it.first }
        val startTime = data["@startat"]!!.single().toDate()
        val contestLength = data["@contlen"]!!.single().toInt().minutes
        val freezeTime = data["@freeze"]!!.single().toInt().minutes
        val contestInfo = ContestInfo(
            name = data["@contest"]!!.single(),
            status = data["@state"]!!.single().toStatus(startTime, contestLength, freezeTime),
            resultType = ContestResultType.ICPC,
            contestLength = contestLength,
            freezeTime = freezeTime,
            teamList = teams,
            problemList = problems,
            penaltyPerWrongAttempt = (penalty.getOrNull(0) ?: 20).minutes,
            penaltyRoundingMode = PenaltyRoundingMode.SUM_DOWN_TO_MINUTE,
            groupList = emptyList(),
            organizationList = emptyList(),
        )
        val runs = (data["@s"] ?: emptyList()).mapIndexed { index, subm ->
            val (teamId, problemId, _, time, verdict) = subm.splitCommas()
            RunInfo(
                id = index.toRunId(),
                result = Verdict.lookup(
                    shortName = verdict,
                    isAccepted = verdict == "OK",
                    isAddingPenalty = when (verdict) {
                        "OK" -> false
                        "CE" -> isCEPenalty
                        else -> true
                    }
                ).takeIf { verdict != "FZ" }?.toICPCRunResult() ?: RunResult.InProgress(0.0),
                problemId = problemId.toProblemId(),
                teamId = teamId.toTeamId(),
                time = time.toInt().seconds,
            )
        }
        return ContestParseResult(
            contestInfo,
            runs,
            emptyList()
        )
    }

    private fun String.splitCommas() = buildList {
        val builder = StringBuilder()
        var inEsc = false
        for (c in this@splitCommas) {
            when {
                !inEsc && c == ',' -> {
                    add(builder.toString())
                    builder.clear()
                }

                c == '"' -> inEsc = !inEsc
                else -> builder.append(c)
            }
        }
        add(builder.toString())
    }

    private val dateFormat = LocalDateTime.Format {
        dayOfMonth()
        char('.')
        monthNumber()
        char('.')
        year()
        char(' ')
        time(LocalTime.Formats.ISO)
    }

    private fun String.toDate(): Instant {
        return dateFormat.parse(this).toInstant(settings.timeZone)
    }

    private fun String.toStatus(startTime: Instant, contestLength: Duration, freezeTime: Duration): ContestStatus = when (this) {
        "RESULTS", "OVER", "OVER (FROZEN)" -> ContestStatus.OVER(startedAt = startTime, finishedAt = startTime + contestLength, frozenAt = startTime + freezeTime)
        "BEFORE" -> ContestStatus.BEFORE(scheduledStartAt = startTime)
        "RUNNING", "FROZEN" -> ContestStatus.RUNNING(startedAt = startTime, frozenAt = (startTime + freezeTime).takeIf { this == "FROZEN" })
        else -> TODO("Unknown contests state $this")
    }

    companion object {
        const val EOF = 26.toByte()
    }
}