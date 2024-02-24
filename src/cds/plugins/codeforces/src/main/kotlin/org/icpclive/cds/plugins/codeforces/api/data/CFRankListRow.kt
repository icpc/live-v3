package org.icpclive.cds.plugins.codeforces.api.data

import kotlinx.serialization.Serializable

@Serializable
internal data class CFRankListRow(
    val party: CFParty,
    val rank: Int,
    val points: Double,
    val penalty: Int,
    val successfulHackCount: Int,
    val unsuccessfulHackCount: Int,
    val problemResults: List<CFProblemResult>,
    val lastSubmissionTimeSeconds: Long? = null,
)