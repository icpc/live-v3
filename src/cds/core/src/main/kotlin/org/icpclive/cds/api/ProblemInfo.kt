package org.icpclive.cds.api

import kotlinx.serialization.*
import org.icpclive.cds.util.serializers.ColorSerializer
import java.awt.Color

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
public data class ProblemInfo(
    val id: ProblemId,
    @SerialName("letter") val displayName: String,
    @SerialName("name") val fullName: String,
    val ordinal: Int,
    @Required val minScore: Double? = null,
    @Required val maxScore: Double? = null,
    @Required @Serializable(ColorSerializer::class) val color: Color? = null,
    @Required @Serializable(ColorSerializer::class) val unsolvedColor: Color? = null,
    @Required val scoreMergeMode: ScoreMergeMode? = null,
    @Required val isHidden: Boolean = false,
)