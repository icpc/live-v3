package org.icpclive.cds.tunning

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.icpclive.cds.api.*
import org.icpclive.cds.util.logger

/**
 * A rule overriding settings of each group separately.
 * If there is override for a non-existent group, a warning is issued.
 * If there is no override for a group, values from the contest system are used.
 *
 * @param rules a map from group id to [Override] for this group. Check [Override] doc for details.
 */
@Serializable
@SerialName("overrideGroups")
public data class OverrideGroups(public val rules: Map<GroupId, Override>): TuningRule {
    @OptIn(InefficientContestInfoApi::class)
    override fun process(info: ContestInfo): ContestInfo {
        return info.copy(
            groupList = mergeOverrides(
                info.groupList,
                rules,
                { id },
                logUnused = { logger(OverrideGroups::class).warning { "No group for override: $it" } }
            ) { group, override ->
                GroupInfo(
                    id = group.id,
                    displayName = override.displayName ?: group.displayName,
                    isHidden = override.isHidden ?: group.isHidden,
                    isOutOfContest = override.isOutOfContest ?: group.isOutOfContest,
                )
            }
        )
    }

    /**
     * An override for a single group
     *
     * All fields can be null, existing values are not changed in that case.
     *
     * @param displayName Name of the group to be displayed in admin and export
     * @param isHidden Totally hide all teams from this group
     * @param isOutOfContest Teams from this group will be visible everywhere, but will not have any rank assigned to them in the leaderboard
     */
    @Serializable
    public class Override(
        public val displayName: String? = null,
        public val isHidden: Boolean? = null,
        public val isOutOfContest: Boolean? = null,
    )
}