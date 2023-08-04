package org.icpclive.api

import kotlinx.serialization.*
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import org.icpclive.util.DurationInMillisecondsSerializer
import kotlin.time.Duration

@Serializable
data class RunInfo(
    val id: Int,
    val result: RunResult?,
    val percentage: Double,
    val problemId: Int,
    val teamId: Int,
    @Serializable(with = DurationInMillisecondsSerializer::class)
    val time: Duration,
    val featuredRunMedia: MediaType? = null,
    val reactionVideos: List<MediaType> = emptyList(),
    val isHidden: Boolean = false,
)

@Serializable
sealed class RunResult

@Serializable(with = VerdictSerializer::class)
sealed class Verdict(val shortName: String, val isAddingPenalty: Boolean, val isAccepted: Boolean) {
    object Accepted: Verdict("AC", false, true)
    object Rejected: Verdict("RJ", true, false)
    object Fail: Verdict("FL", false, true)
    object CompilationError: Verdict("CE", false, false)
    object CompilationErrorWithPenalty: Verdict("CE", true, false)
    object PresentationError: Verdict("PE", true, false)
    object RuntimeError: Verdict("RE", true, false)
    object TimeLimitExceeded: Verdict("TL", true, false)
    object MemoryLimitExceeded: Verdict("ML", true, false)
    object OutputLimitExceeded: Verdict("OL", true, false)
    object IdlenessLimitExceeded: Verdict("IL", true, false)
    object SecurityViolation: Verdict("SV", true, false)
    object Ignored: Verdict("IG", false, false)
    object Challenged: Verdict("CH", true, false)
    object WrongAnswer: Verdict("WA", true, false)

    // We have a separate object here to delay initialization until the function call.
    // This allows first fully initialize Verdict subclasses, which is required by LookupHolder constructor
    companion object {
        val all get() = LookupHolder.all
        fun lookup(shortName: String, isAddingPenalty: Boolean, isAccepted: Boolean) =
            LookupHolder.lookup(shortName, isAddingPenalty, isAccepted)
    }

    fun toRunResult() = ICPCRunResult(this, false)
}


class VerdictSerializer : KSerializer<Verdict> {
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


    fun lookup(shortName: String, isAddingPenalty: Boolean, isAccepted: Boolean) : Verdict {
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
@SerialName("ICPC")
data class ICPCRunResult(
    val verdict: Verdict,
    val isFirstToSolveRun: Boolean,
) : RunResult()

@Serializable
@SerialName("IOI")
data class IOIRunResult(
    val score: List<Double>,
    val wrongVerdict: Verdict? = null,
    val difference: Double = 0.0,
    val scoreAfter: Double = 0.0,
    val isFirstBestRun: Boolean = false,
    val isFirstBestTeamRun: Boolean = false
) : RunResult()
