@file:Suppress("UNUSED")

package org.icpclive.cds.codeforces.api.data

import kotlinx.serialization.Serializable

enum class CFContestType {
    CF, IOI, ICPC
}

enum class CFContestPhase {
    BEFORE, CODING, PENDING_SYSTEM_TEST, SYSTEM_TEST, FINISHED
}

@Serializable
data class CFContest(
    val id: Int,
    val name: String,
    val type: CFContestType,
    val phase: CFContestPhase,
    val frozen: Boolean,
    val durationSeconds: Long? = null,
    val startTimeSeconds: Long? = null,
    val relativeTimeSeconds: Long? = null,
    val preparedBy: String? = null,
    val websiteUrl: String? = null,
    val description: String? = null,
    val difficulty: Int? = null,
    val kind: String? = null,
    val icpcRegion: String? = null,
    val country: String? = null,
    val city: String? = null,
    val season: String? = null,
)