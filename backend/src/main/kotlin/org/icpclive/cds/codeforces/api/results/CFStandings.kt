package org.icpclive.cds.codeforces.api.results

import org.icpclive.cds.codeforces.api.data.CFContest
import org.icpclive.cds.codeforces.api.data.CFProblem
import org.icpclive.cds.codeforces.api.data.CFRankListRow
import kotlinx.serialization.Serializable

@Serializable
data class CFStandings(
    val contest: CFContest,
    val problems: List<CFProblem>,
    val rows: List<CFRankListRow>,
)