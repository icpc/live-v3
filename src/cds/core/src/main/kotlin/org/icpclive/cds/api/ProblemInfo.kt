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
@SerialName("ftsMode")
public data class FtsMode(val type: FtsModeType, val runId: RunId? = null) {
    @Serializable
    public enum class FtsModeType {
        @SerialName("auto")
        AUTO,
        @SerialName("hide")
        HIDE,
        @SerialName("custom")
        CUSTOM,
    }
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
    @Required val unsolvedColor: Color? = null,
    @Required val scoreMergeMode: ScoreMergeMode? = null,
    @Required val isHidden: Boolean = false,
    @Required val weight: Int = 1,
    @Required val ftsMode: FtsMode = FtsMode(FtsMode.FtsModeType.AUTO),
)

@JvmInline
@Serializable(with = ColorSerializer::class)
public value class Color internal constructor(public val value: String) {
    public companion object {
        private val log by getLogger()
        public fun normalize(data: String): Color? {
            val red: Int
            val green: Int
            val blue: Int
            val alpha: Int
            try {
                if (data.startsWith("0x")) {
                    val colorValue = data.toUInt(radix = 16).toInt()
                    red = ((colorValue ushr 16) and 0xFF)
                    green = ((colorValue ushr 8) and 0xFF)
                    blue = ((colorValue ushr 0) and 0xFF)
                    alpha = if (data.length == 8) ((colorValue ushr 24) and 0xFF) else 0xff
                } else {
                    val str = data.removePrefix("#")
                    when (str.length) {
                        8 -> {
                            red = str.substring(0, 2).toInt(radix = 16)
                            green = str.substring(2, 4).toInt(radix = 16)
                            blue = str.substring(4, 6).toInt(radix = 16)
                            alpha = str.substring(6, 8).toInt(radix = 16)
                        }

                        6 -> {
                            red = str.substring(0, 2).toInt(radix = 16)
                            green = str.substring(2, 4).toInt(radix = 16)
                            blue = str.substring(4, 6).toInt(radix = 16)
                            alpha = 255
                        }

                        3 -> {
                            red = str[0].digitToInt(16) * 0x11
                            green = str[1].digitToInt(16) * 0x11
                            blue = str[2].digitToInt(16) * 0x11
                            alpha = 255
                        }

                        else -> throw NumberFormatException()
                    }
                }
            } catch (e: NumberFormatException) {
                log.error(e) { "Failed to parse color from $data" }
                return null
            }
            return Color("#%02x%02x%02x%02x".format(red, green, blue, alpha))
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