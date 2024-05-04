@file:Suppress("UNUSED")

package org.icpclive.cds.plugins.codeforces.api.data

import kotlinx.datetime.Instant
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.icpclive.cds.util.serializers.DurationInSecondsSerializer
import org.icpclive.cds.util.serializers.UnixSecondsSerializer
import kotlin.time.Duration

internal enum class CFContestType {
    CF, IOI, ICPC
}

internal enum class CFContestPhase {
    BEFORE, CODING, PENDING_SYSTEM_TEST, SYSTEM_TEST, FINISHED
}

@Serializable
internal data class CFContest(
    val id: Int,
    val name: String,
    val type: CFContestType,
    val phase: CFContestPhase,
    val frozen: Boolean,
    @Serializable(DurationInSecondsSerializer::class)
    @SerialName("durationSeconds")
    val duration: Duration? = null,
    @Serializable(UnixSecondsSerializer::class)
    @SerialName("startTimeSeconds")
    val startTime: Instant? = null,
    @Serializable(DurationInSecondsSerializer::class)
    @SerialName("relativeTimeSeconds")
    val relativeTime: Duration? = null,
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