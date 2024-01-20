@file:Suppress("UNUSED")

package org.icpclive.cds.plugins.codeforces.api.data

import kotlinx.serialization.Serializable

internal enum class CFProblemResultType {
    PRELIMINARY, FINAL
}

@Serializable
internal data class CFProblemResult(
    val points: Double,
    val penalty: Int? = null,
    val rejectedAttemptCount: Int,
    val type: CFProblemResultType,
    val bestSubmissionTimeSeconds: Long? = null,
)