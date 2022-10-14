package org.icpclive.cds.codeforces.api.results

import kotlinx.serialization.Serializable
import org.icpclive.cds.codeforces.api.data.CFContest
import org.icpclive.cds.codeforces.api.data.CFProblem
import org.icpclive.cds.codeforces.api.data.CFRankListRow

@Serializable
data class CFStandings(
    val contest: CFContest,
    val problems: List<CFProblem>,
    val rows: List<CFRankListRow>,
)