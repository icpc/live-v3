@file:Suppress("UNUSED")

package org.icpclive.cds.plugins.codeforces.api.data

import kotlinx.serialization.Serializable
import org.icpclive.cds.util.serializers.DurationInSecondsSerializer
import kotlin.time.Duration

internal enum class CFSubmissionVerdict {
    FAILED, OK, PARTIAL, COMPILATION_ERROR, RUNTIME_ERROR, WRONG_ANSWER, PRESENTATION_ERROR, TIME_LIMIT_EXCEEDED, MEMORY_LIMIT_EXCEEDED, IDLENESS_LIMIT_EXCEEDED, SECURITY_VIOLATED, CRASHED, INPUT_PREPARATION_CRASHED, CHALLENGED, SKIPPED, TESTING, REJECTED
}

internal enum class CFSubmissionTestSet {
    SAMPLES, PRETESTS, TESTS, CHALLENGES, TESTS1, TESTS2, TESTS3, TESTS4, TESTS5, TESTS6, TESTS7, TESTS8, TESTS9, TESTS10
}

@Serializable
internal data class CFSubmission(
    val id: Long,
    val contestId: Int? = null,
    val creationTimeSeconds: Long,
    @Serializable(DurationInSecondsSerializer::class)
    val relativeTimeSeconds: Duration,
    val problem: CFProblem,
    val author: CFParty,
    val programmingLanguage: String,
    val verdict: CFSubmissionVerdict? = null,
    val testset: CFSubmissionTestSet? = null,
    val passedTestCount: Int,
    val timeConsumedMillis: Int,
    val memoryConsumedBytes: Long,
    val points: Double? = null,
)
