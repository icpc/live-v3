package org.icpclive.cds.codeforces.api.results

import kotlinx.serialization.Serializable
import org.icpclive.cds.codeforces.api.data.CFSubmission


@JvmInline
@Serializable
value class CFSubmissionList(val list: List<CFSubmission>)
