@file:Suppress("UNUSED")

package org.icpclive.cds.plugins.codeforces.api.data

import kotlinx.serialization.Serializable

internal enum class CFProblemType {
    PROGRAMMING, QUESTION
}


@Serializable
internal data class CFProblem(
    val contestId: Int,
    val problemsetName: String? = null,
    val index: String,
    val name: String? = null,
    val type: CFProblemType,
    val points: Double? = null,
    val rating: Int? = null,
    val tags: List<String>? = null,
)