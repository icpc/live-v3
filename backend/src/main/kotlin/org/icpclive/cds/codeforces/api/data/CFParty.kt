@file:Suppress("UNUSED")
package org.icpclive.cds.codeforces.api.data

import kotlinx.serialization.*

enum class CFPartyParticipantType {
    CONTESTANT, PRACTICE, VIRTUAL, MANAGER, OUT_OF_COMPETITION
}

@Serializable
data class CFParty(
    val contestId: Int? = null,
    val members: List<CFMember>,
    val participantType: CFPartyParticipantType,
    val teamId: Int? = null,
    val teamName: String? = null,
    val ghost: Boolean,
    val room: Int? = null,
    val startTimeSeconds: Long? = null,
)