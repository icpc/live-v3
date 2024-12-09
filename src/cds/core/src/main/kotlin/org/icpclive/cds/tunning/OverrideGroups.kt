package org.icpclive.cds.tunning

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.icpclive.cds.api.*
import org.icpclive.cds.util.getLogger

@Serializable
@SerialName("override_groups")
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