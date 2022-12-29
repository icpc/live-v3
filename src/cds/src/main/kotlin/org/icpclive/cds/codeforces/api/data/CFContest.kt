@file:Suppress("UNUSED")

package org.icpclive.cds.codeforces.api.data

import kotlinx.serialization.Serializable
import org.icpclive.util.DurationInSecondsSerializer
import kotlin.time.Duration

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
    @Serializable(DurationInSecondsSerializer::class)
    val durationSeconds: Duration? = null,
    val startTimeSeconds: Long? = null,
    @Serializable(DurationInSecondsSerializer::class)
    val relativeTimeSeconds: Duration? = null,
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