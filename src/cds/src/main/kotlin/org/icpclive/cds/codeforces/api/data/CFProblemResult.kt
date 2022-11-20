@file:Suppress("UNUSED")

package org.icpclive.cds.codeforces.api.data

import kotlinx.serialization.Serializable

enum class CFProblemResultType {
    PRELIMINARY, FINAL
}

@Serializable
data class CFProblemResult(
    val points: Double,
    val penalty: Int? = null,
    val rejectedAttemptCount: Int,
    val type: CFProblemResultType,
    val bestSubmissionTimeSeconds: Long? = null,
)