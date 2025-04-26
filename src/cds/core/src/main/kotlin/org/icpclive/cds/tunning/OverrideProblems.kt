package org.icpclive.cds.tunning

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.icpclive.cds.api.*
import org.icpclive.cds.util.logger

/**
 * A rule overriding settings of each problem separately.
 * If there is override for a non-existent problem, a warning is issued.
 * If there is no override for a problem, values from the contest system are used.
 *
 * @param rules a map from problem id to [Override] for this problem. Check [Override] doc for details.
 */
@Serializable
@SerialName("overrideProblems")
public data class OverrideProblems(public val rules: Map<ProblemId, Override>): TuningRule {
    @OptIn(InefficientContestInfoApi::class)
    override fun process(info: ContestInfo): ContestInfo {
        return info.copy(
            problemList = mergeOverrides(
                info.problemList,
                rules,
                { id },
                logUnused = { logger(OverrideProblems::class).warning { "No problem for override: $it" } }
            ) { problem, override ->
                problem.copy(
                    displayName = override.displayName ?: problem.displayName,
                    fullName = override.fullName ?: problem.fullName,
                    ordinal = override.ordinal ?: problem.ordinal,
                    minScore = override.minScore ?: problem.minScore,
                    maxScore = override.maxScore ?: problem.maxScore,
                    color = override.color ?: problem.color,
                    scoreMergeMode = override.scoreMergeMode ?: problem.scoreMergeMode,
                    isHidden = override.isHidden ?: problem.isHidden,
                    weight = override.weight ?: problem.weight,
                    ftsMode = override.ftsMode ?: problem.ftsMode,
                )
            }
        )
    }

    /**
     * An override for a single problem
     *
     * All fields can be null, existing values are not changed in that case.
     *
     * @param displayName Name to show in scoreboard and queue.
     * @param fullName Problem name.
     * @param color Color of a problem balloon. It would be shown in queue and scoreboard in places related to the problem
     * @param ordinal Number to sort problems in the scoreboard
     * @param minScore In ioi mode minimal possible value of points in this problem
     * @param maxScore In ioi mode maximal possible value of points in this problem
     * @param scoreMergeMode In ioi mode, select the ruleset to calculate the final score based on the scores for each submission.
     * @param isHidden If true, ignore all runs on that problem and remove it from the scoreboard, and hide the corresponding column.
     * @param weight in icpc mode count as this number of problems
     * @param ftsMode Defines how the first to solve is determined and displayed for the problem.
     */
    @Serializable
    public class Override(
        public val displayName: String? = null,
        public val fullName: String? = null,
        public val color: Color? = null,
        public val ordinal: Int? = null,
        public val minScore: Double? = null,
        public val maxScore: Double? = null,
        public val scoreMergeMode: ScoreMergeMode? = null,
        public val isHidden: Boolean? = null,
        public val weight: Int? = null,
        public val ftsMode: FtsMode? = null,
    )
}