package org.icpclive.cds.api

import kotlinx.serialization.*
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import org.icpclive.cds.util.serializers.DurationInMillisecondsSerializer
import kotlin.time.Duration

@JvmInline
@Serializable
public value class RunId internal constructor(public val value: String) {
    override fun toString(): String = value
}

public fun String.toRunId(): RunId = RunId(this)
public fun Int.toRunId(): RunId = toString().toRunId()
public fun Long.toRunId(): RunId = toString().toRunId()



@Serializable
public data class RunInfo(
    val id: RunId,
    val result: RunResult,
    val problemId: ProblemId,
    val teamId: TeamId,
    @Serializable(with = DurationInMillisecondsSerializer::class)
    val time: Duration,
    val languageId: LanguageId?,
    @Serializable(with = DurationInMillisecondsSerializer::class)
    val testedTime: Duration? = null,
    @Required val featuredRunMedia: MediaType? = null,
    @Required val reactionVideos: List<MediaType> = emptyList(),
    @Required val isHidden: Boolean = false,
    val sourceFiles: List<MediaType> = emptyList(),
)

@Serializable(with = VerdictSerializer::class)
public sealed class Verdict(
    public val shortName: String,
    public val isAddingPenalty: Boolean,
    public val isAccepted: Boolean,
) {
    public data object Accepted : Verdict("AC", false, true)
    public data object Rejected : Verdict("RJ", true, false)
    public data object Fail : Verdict("FL", false, true)
    public data object CompilationError : Verdict("CE", false, false)
    public data object CompilationErrorWithPenalty : Verdict("CE", true, false)
    public data object PresentationError : Verdict("PE", true, false)
    public data object RuntimeError : Verdict("RE", true, false)
    public data object TimeLimitExceeded : Verdict("TL", true, false)
    public data object MemoryLimitExceeded : Verdict("ML", true, false)
    public data object OutputLimitExceeded : Verdict("OL", true, false)
    public data object IdlenessLimitExceeded : Verdict("IL", true, false)
    public data object SecurityViolation : Verdict("SV", true, false)
    public data object Ignored : Verdict("IG", false, false)
    public data object Challenged : Verdict("CH", true, false)
    public data object WrongAnswer : Verdict("WA", true, false)

    public companion object {
        public val all: List<Verdict> get() = LookupHolder.all
        public fun lookup(shortName: String, isAddingPenalty: Boolean, isAccepted: Boolean): Verdict =
            LookupHolder.lookup(shortName, isAddingPenalty, isAccepted)
    }

    public fun toICPCRunResult(): RunResult.ICPC = RunResult.ICPC(this, false)
}


internal class VerdictSerializer : KSerializer<Verdict> {
    @Serializable
    @SerialName("Verdict")
    private class VerdictSurrogate(val shortName: String, val isAddingPenalty: Boolean, val isAccepted: Boolean)

    override val descriptor = VerdictSurrogate.serializer().descriptor
    override fun deserialize(decoder: Decoder): Verdict {
        val surrogate = decoder.decodeSerializableValue(VerdictSurrogate.serializer())
        return Verdict.lookup(surrogate.shortName, surrogate.isAddingPenalty, surrogate.isAccepted)
    }

    override fun serialize(encoder: Encoder, value: Verdict) {
        val surrogate = VerdictSurrogate(value.shortName, value.isAddingPenalty, value.isAccepted)
        encoder.encodeSerializableValue(VerdictSurrogate.serializer(), surrogate)
    }
}

// We have a separate object here to delay initialization until the function call.
// This allows first fully initializing Verdict subclasses, which is required by LookupHolder constructor
private object LookupHolder {
    val all = Verdict::class.sealedSubclasses.map { it.objectInstance!! }

    private val alternativeNames = listOf(
        "OK" to Verdict.Accepted,
        "TLE" to Verdict.TimeLimitExceeded,
        "RT" to Verdict.RuntimeError,
        "RTE" to Verdict.RuntimeError,
        "OLE" to Verdict.OutputLimitExceeded,
        "MLE" to Verdict.MemoryLimitExceeded,
        "ILE" to Verdict.IdlenessLimitExceeded,
        "WTL" to Verdict.IdlenessLimitExceeded,
        "CTL" to Verdict.CompilationError,
    )

    private val mainNames = all.map {
        it.shortName to it
    }

    private val allNames = (alternativeNames + mainNames).groupBy({ it.first }, { it.second })


    fun lookup(shortName: String, isAddingPenalty: Boolean, isAccepted: Boolean): Verdict {
        val found = allNames[shortName]?.singleOrNull { it.isAddingPenalty == isAddingPenalty && it.isAccepted == isAccepted }
        return when {
            found != null -> found
            isAccepted -> Verdict.Accepted
            isAddingPenalty -> Verdict.Rejected
            else -> Verdict.Ignored
        }
    }
}


@Serializable
public sealed class RunResult {
    @Serializable
    @SerialName("ICPC")
    public data class ICPC(
        val verdict: Verdict,
        val isFirstToSolveRun: Boolean,
    ) : RunResult()

    @Serializable
    @SerialName("IOI")
    public data class IOI(
        val score: List<Double>,
        @Required val wrongVerdict: Verdict? = null,
        @Required val difference: Double = 0.0,
        @Required val scoreAfter: Double = 0.0,
        @Required val isFirstBestRun: Boolean = false,
        @Required val isFirstBestTeamRun: Boolean = false,
    ) : RunResult()

    @Serializable
    @SerialName("IN_PROGRESS")
    public data class InProgress(val testedPart: Double): RunResult()
}

