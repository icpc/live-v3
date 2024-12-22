package org.icpclive.cds.tunning

import kotlinx.serialization.*
import org.icpclive.cds.adapters.impl.autoCreateGroupsAndOrgs
import org.icpclive.cds.api.*
import org.icpclive.cds.util.getLogger

@Serializable
@SerialName("override_teams")
public data class OverrideTeams(public val rules: Map<TeamId, TeamInfoOverride>): TuningRule {
    @OptIn(InefficientContestInfoApi::class)
    override fun process(info: ContestInfo, submittedTeams: Set<TeamId>): ContestInfo {
        val newTeams = mergeOverrides(
            info.teamList,
            rules,
            { id },
            logUnused = { logger.warning { "No team for override: $it" } },
        ) { team, override ->
            TeamInfo(
                id = team.id,
                fullName = override.fullName ?: team.fullName,
                displayName = override.displayName ?: team.displayName,
                groups = (override.groups ?: team.groups) + override.extraGroups.orEmpty(),
                hashTag = override.hashTag ?: team.hashTag,
                medias = mergeMaps(team.medias, override.medias ?: emptyMap()),
                customFields = mergeMaps(team.customFields, override.customFields ?: emptyMap()),
                isHidden = override.isHidden ?: team.isHidden,
                isOutOfContest = override.isOutOfContest ?: team.isOutOfContest,
                organizationId = override.organizationId ?: team.organizationId,
                color = override.color ?: team.color,
            )
        }
        return info.copy(teamList = newTeams).autoCreateGroupsAndOrgs()
    }

    private companion object {
        val logger by getLogger()
    }
}