package org.icpclive.cds.tunning

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.icpclive.cds.api.*
import org.icpclive.cds.util.getLogger

/**
 * @param displayName Name to show in scoreboard and queue.
 * @param fullName Problem name.
 * @param color Color of a problem balloon. It would be shown in queue and scoreboard in places related to the problem
 * @param ordinal Number to sort problems in the scoreboard
 * @param minScore In ioi mode minimal possible value of points in this problem
 * @param maxScore In ioi mode maximal possible value of points in this problem
 * @param scoreMergeMode In ioi mode, select the ruleset to calculate the final score based on the scores for each submission.
 * @param isHidden If true, ignore all runs on that problem and remove it from scoreboard.
 */
@Serializable
public class ProblemInfoOverride(
    public val displayName: String? = null,
    public val fullName: String? = null,
    public val color: Color? = null,
    public val unsolvedColor: Color? = null,
    public val ordinal: Int? = null,
    public val minScore: Double? = null,
    public val maxScore: Double? = null,
    public val scoreMergeMode: ScoreMergeMode? = null,
    public val isHidden: Boolean? = null,
)

@Serializable
@SerialName("overrideProblems")
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