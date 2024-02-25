package org.icpclive.cds.plugins.codeforces.api.results

import kotlinx.serialization.Serializable
import org.icpclive.cds.plugins.codeforces.api.data.*

@Serializable
internal data class CFStandings(
    val contest: CFContest,
    val problems: List<CFProblem>,
    val rows: List<CFRankListRow>,
)