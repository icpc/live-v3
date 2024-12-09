package org.icpclive.cds.tunning

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.icpclive.cds.api.*
import org.icpclive.cds.util.getLogger

@Serializable
@SerialName("override_problems")
public data class OverrideProblems(public val rules: Map<ProblemId, ProblemInfoOverride>): TuningRule {
    @OptIn(InefficientContestInfoApi::class)
    override fun process(info: ContestInfo, submittedTeams: Set<TeamId>): ContestInfo {
        return info.copy(
            problemList = mergeOverrides(
                info.problemList,
                rules,
                { id },
                logUnused = { logger.warning { "No problem for override: $it" } }
            ) { problem, override ->
                ProblemInfo(
                    id = problem.id,
                    displayName = override.displayName ?: problem.displayName,
                    fullName = override.fullName ?: problem.fullName,
                    ordinal = override.ordinal ?: problem.ordinal,
                    minScore = override.minScore ?: problem.minScore,
                    maxScore = override.maxScore ?: problem.maxScore,
                    color = override.color ?: problem.color,
                    unsolvedColor = override.unsolvedColor ?: problem.unsolvedColor,
                    scoreMergeMode = override.scoreMergeMode ?: problem.scoreMergeMode,
                    isHidden = override.isHidden ?: problem.isHidden,
                )
            }
        )
    }

    private companion object {
        val logger by getLogger()
    }
}