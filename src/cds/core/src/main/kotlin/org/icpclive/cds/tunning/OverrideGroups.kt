package org.icpclive.cds.tunning

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.icpclive.cds.api.*
import org.icpclive.cds.util.getLogger

/**
 * @param displayName Name of the group to be displayed in admin and export
 * @param isHidden Totally hide all teams from this group
 * @param isOutOfContest Teams from this group will be visible everywhere, but will not have any rank assigned to them in the leaderboard
 */
@Serializable
public class GroupInfoOverride(
    public val displayName: String? = null,
    public val isHidden: Boolean? = null,
    public val isOutOfContest: Boolean? = null,
)


@Serializable
@SerialName("overrideGroups")
public data class OverrideGroups(public val rules: Map<GroupId, GroupInfoOverride>): TuningRule {
    @OptIn(InefficientContestInfoApi::class)
    override fun process(info: ContestInfo, submittedTeams: Set<TeamId>): ContestInfo {
        return info.copy(
            groupList = mergeOverrides(
                info.groupList,
                rules,
                { id },
                logUnused = { logger.warning { "No group for override: $it" } }
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

    private companion object {
        val logger by getLogger()
    }
}