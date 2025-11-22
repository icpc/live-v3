package org.icpclive.cds.api

import kotlinx.serialization.*
import kotlinx.serialization.descriptors.*
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import org.icpclive.cds.util.getLogger

@JvmInline
@Serializable
public value class ProblemId internal constructor(public val value: String) {
    override fun toString(): String = value
}

public fun String.toProblemId(): ProblemId = ProblemId(this)
public fun Int.toProblemId(): ProblemId = toString().toProblemId()
public fun Long.toProblemId(): ProblemId = toString().toProblemId()


public enum class ScoreMergeMode {
    /**
     * For each tests group in the problem, get maximum score over all submissions.
     */
    MAX_PER_GROUP,

    /**
     * Get maximum total score over all submissions
     */
    MAX_TOTAL,

    /**
     * Get score from last submission
     */
    LAST,

    /**
     * Get score from last submissions, ignoring submissions, which didn't pass preliminary testing (e.g. on sample tests)
     */
    LAST_OK,

    /**
     * Get the sum of scores over all submissions
     */
    SUM
}

@Serializable
public sealed class FtsMode {
    @Serializable
    @SerialName("auto")
    public object Auto : FtsMode()
    @Serializable
    @SerialName("hidden")
    public object Hidden : FtsMode()
    @Serializable
    @SerialName("custom")
    public data class Custom(val runId: RunId) : FtsMode()
}

@Serializable
public data class ProblemInfo(
    val id: ProblemId,
    @SerialName("letter") val displayName: String,
    @SerialName("name") val fullName: String,
    val ordinal: Int,
    @Required val minScore: Double? = null,
    @Required val maxScore: Double? = null,
    @Required val color: Color? = null,
    @Required val scoreMergeMode: ScoreMergeMode? = null,
    @Required val isHidden: Boolean = false,
    @Required val weight: Int = 1,
    @Required val ftsMode: FtsMode = FtsMode.Auto,
)

@JvmInline
@Serializable(with = ColorSerializer::class)
public value class Color internal constructor(public val value: String) {
    public companion object {
        private val log by getLogger()
        private fun String.twice() = flatMap { listOf(it, it) }.joinToString("")
        public fun normalize(data: String): Color? {
            return runCatching {
                 val colorValue = if (data.startsWith("0x")) {
                    val r = data.removePrefix("0x").toUInt(radix = 16)
                    if (data.length == 10) {
                        r.rotateLeft(8)
                    } else {
                        (r shl 8) or 0xFFu
                    }
                } else {
                    val str = data.removePrefix("#")
                    when (str.length) {
                        8 -> str.toUInt(radix = 16)
                        6 -> (str+"FF").toUInt(radix = 16)
                        3 -> (str+"F").twice().toUInt(radix = 16)
                        4 -> str.twice().toUInt(radix = 16)
                        else -> throw NumberFormatException("Invalid color string length")
                    }
                }
                Color("#%06x".format(colorValue.toInt() ushr 8))
            }.getOrElse {  e ->
                log.error(e) { "Failed to parse color from $data" }
                null
            }
        }
    }
}


internal object ColorSerializer : KSerializer<Color> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("Color", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: Color) {
        encoder.encodeString(value.value)
    }

    override fun deserialize(decoder: Decoder): Color {
        return Color.normalize(decoder.decodeString()) ?: Color("#000000")
    }
}