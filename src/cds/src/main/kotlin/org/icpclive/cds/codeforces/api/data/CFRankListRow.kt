package org.icpclive.cds.codeforces.api.data

import kotlinx.serialization.Serializable

@Serializable
data class CFRankListRow(
    val party: CFParty,
    val rank: Int,
    val points: Double,
    val penalty: Int,
    val successfulHackCount: Int,
    val unsuccessfulHackCount: Int,
    val problemResults: List<CFProblemResult>,
    val lastSubmissionTimeSeconds: Long? = null,
)