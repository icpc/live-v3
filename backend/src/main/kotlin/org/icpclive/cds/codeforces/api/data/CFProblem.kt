@file:Suppress("UNUSED")
package org.icpclive.cds.codeforces.api.data

import kotlinx.serialization.*

enum class CFProblemType {
    PROGRAMMING, QUESTION
}


@Serializable
data class CFProblem(
    val contestId: Int,
    val problemsetName: String? = null,
    val index: String,
    val name: String? = null,
    val type: CFProblemType,
    val points: Double? = null,
    val rating: Int? = null,
    val tags: List<String>? = null,
)