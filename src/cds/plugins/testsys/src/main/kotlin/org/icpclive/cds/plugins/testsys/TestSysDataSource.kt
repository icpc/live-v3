package org.icpclive.cds.plugins.testsys

import kotlinx.datetime.*
import kotlinx.datetime.format.char
import org.icpclive.cds.*
import org.icpclive.cds.api.*
import org.icpclive.cds.ktor.*
import org.icpclive.cds.settings.CDSSettings
import org.icpclive.cds.settings.UrlOrLocalPath
import org.icpclive.ksp.cds.Builder
import java.nio.charset.Charset
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Instant

@Builder("testsys")
public sealed interface TestSysSettings : CDSSettings, KtorNetworkSettingsProvider {
    public val source: UrlOrLocalPath
    public val timeZone: TimeZone
        get() = TimeZone.of("Europe/Moscow")
    public val resultType: ContestResultType
        get() = ContestResultType.ICPC

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

    private val oldScores = mutableMapOf<RunId, Double>()

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
                scoreMergeMode = ScoreMergeMode.LAST_OK.takeIf { settings.resultType == ContestResultType.IOI },
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
            resultType = settings.resultType,
            contestLength = contestLength,
            freezeTime = freezeTime,
            teamList = teams,
            problemList = problems,
            penaltyPerWrongAttempt = (penalty.getOrNull(0) ?: 20).minutes,
            penaltyRoundingMode = if (settings.resultType == ContestResultType.IOI) PenaltyRoundingMode.ZERO else PenaltyRoundingMode.SUM_DOWN_TO_MINUTE,
            groupList = emptyList(),
            organizationList = emptyList(),
            languagesList = emptyList()
        )
        val runs = (data["@s"] ?: emptyList()).mapIndexed { index, subm ->
            val (teamId, problemId, _, time, verdict) = subm.splitCommas()
            RunInfo(
                id = index.toRunId(),
                result = parseVerdict(index.toRunId(), verdict, isCEPenalty),
                problemId = problemId.toProblemId(),
                teamId = teamId.toTeamId(),
                time = time.toInt().seconds,
                languageId = null
            )
        }
        if (settings.resultType == ContestResultType.IOI) {
            for (r in runs) {
                val result = r.result as? RunResult.IOI ?: continue
                if (result.wrongVerdict != null) continue
                oldScores[r.id] = result.score[0]
            }
        }
        return ContestParseResult(
            contestInfo,
            runs,
            emptyList()
        )
    }

    private fun parseVerdict(id: RunId, verdict: String, isCEPenalty: Boolean) : RunResult {
        if (verdict == "??") return RunResult.InProgress(0.0)
        return when (settings.resultType) {
            ContestResultType.ICPC -> Verdict.lookup(
                shortName = verdict,
                isAccepted = verdict == "OK",
                isAddingPenalty = when (verdict) {
                    "OK" -> false
                    "CE" -> isCEPenalty
                    else -> true
                }
            ).takeIf { verdict != "FZ" }?.toICPCRunResult() ?: RunResult.InProgress(0.0)

            ContestResultType.IOI -> if (verdict == "--") {
                val old = oldScores[id]
                if (old == null) {
                    RunResult.IOI(wrongVerdict = Verdict.Ignored, score = listOf())
                } else {
                    RunResult.IOI(score = listOf(old))
                }
            } else {
                RunResult.IOI(listOf(verdict.toDouble()))
            }
        }
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
        day()
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