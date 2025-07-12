package org.icpclive.cds.plugins.codeforces.api.data

import kotlinx.serialization.Serializable
import org.icpclive.cds.util.serializers.UnixSecondsSerializer
import kotlin.time.Instant

internal enum class CFHackVerdict {
    HACK_SUCCESSFUL, HACK_UNSUCCESSFUL, INVALID_INPUT, GENERATOR_INCOMPILABLE, GENERATOR_CRASHED, IGNORED, TESTING, OTHER
}

@Serializable
internal data class CFHack(
    val id: Int,
    @Serializable(UnixSecondsSerializer::class)
    val creationTimeSeconds: Instant,
    val hacker: CFParty,
    val defender: CFParty,
    val verdict: CFHackVerdict? = null,
    val problem: CFProblem,
)