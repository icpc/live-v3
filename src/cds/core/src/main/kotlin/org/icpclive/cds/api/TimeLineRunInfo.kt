package org.icpclive.cds.api

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.icpclive.cds.util.serializers.DurationInMillisecondsSerializer
import kotlin.time.Duration

@Serializable
public sealed class TimeLineRunInfo {
    @Serializable
    @SerialName("ICPC")
    public data class ICPC(@Serializable(with = DurationInMillisecondsSerializer::class) val time: Duration, val problemId: ProblemId, val isAccepted: Boolean) : TimeLineRunInfo()

    @Serializable
    @SerialName("IOI")
    public data class IOI(@Serializable(with = DurationInMillisecondsSerializer::class) val time: Duration, val problemId: ProblemId, val score: Double) : TimeLineRunInfo()

    @Serializable
    @SerialName("IN_PROGRESS")
    public data class InProgress(@Serializable(with = DurationInMillisecondsSerializer::class) val time: Duration, val problemId: ProblemId) : TimeLineRunInfo()

    public companion object {
        public fun fromRunInfo(info: RunInfo, acceptedProblems: MutableSet<ProblemId>): TimeLineRunInfo? {
            return when (info.result) {
                is RunResult.ICPC -> {
                    val icpcResult = info.result
                    if (!acceptedProblems.contains(info.problemId)) {
                        if (icpcResult.verdict.isAccepted) {
                            acceptedProblems.add(info.problemId)
                        }
                        ICPC(info.time, info.problemId, icpcResult.verdict.isAccepted)
                    } else {
                        null
                    }
                }

                is RunResult.IOI -> {
                    val ioiResult = info.result
                    if (ioiResult.difference > 0) {
                        IOI(info.time, info.problemId, ioiResult.score.sum())
                    } else {
                        null
                    }
                }

                else -> {
                    InProgress(info.time, info.problemId)
                }
            }
        }
    }
}
