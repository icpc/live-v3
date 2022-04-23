package org.icpclive.cds.yandex.api

import kotlinx.serialization.Serializable
import org.icpclive.api.RunInfo

@Serializable
data class Submissions(
    val submissions: List<Submission>
)

@Serializable
data class Submission(
    val id: Long,
    val authorId: Long,
    val author: String,
    val compiler: String,
    val problemId: String,
    val problemAlias: String,
    val verdict: String,
    val
) {
    fun toRun(): RunInfo {
        val result = getResult(verdict)

        return RunInfo(
            id = id.toInt(),
            isAccepted = result == "OK",
            isJudged = result != "",
            isAddingPenalty = listOf("OK", "Compi").contains(verdict),
            result = result,
            problemId = problemAlias,
            teamId = teamId,
            percentage = percentage,
            time = time.inWholeMilliseconds,
            isFirstSolvedRun = false
        )
    }

}
