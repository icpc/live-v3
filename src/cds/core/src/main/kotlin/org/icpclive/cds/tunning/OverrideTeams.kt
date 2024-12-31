package org.icpclive.cds.tunning

import kotlinx.serialization.*
import org.icpclive.cds.adapters.impl.autoCreateGroupsAndOrgs
import org.icpclive.cds.api.*
import org.icpclive.cds.util.getLogger
import org.icpclive.cds.util.logger

/**
 * A rule overriding settings of each team separately.
 * If there is override for a non-existent team, a warning is issued.
 * If there is no override for a team, values from the contest system are used.
 *
 * @param rules a map from team id to [Override] for this team. Check [Override] doc for details.
 */
@Serializable
@SerialName("overrideTeams")
public data class OverrideTeams(public val rules: Map<TeamId, Override>): TuningRule {
    @OptIn(InefficientContestInfoApi::class)
    override fun process(info: ContestInfo, submittedTeams: Set<TeamId>): ContestInfo {
        val newTeams = mergeOverrides(
            info.teamList,
            rules,
            { id },
            logUnused = { logger(OverrideTeams::class).warning { "No team for override: $it" } },
        ) { team, override ->
            TeamInfo(
                id = team.id,
                fullName = override.fullName ?: team.fullName,
                displayName = override.displayName ?: team.displayName,
                groups = (override.groups ?: team.groups) + override.extraGroups.orEmpty(),
                hashTag = override.hashTag ?: team.hashTag,
                medias = mergeMaps(team.medias, override.medias),
                customFields = mergeMaps(team.customFields, override.customFields),
                isHidden = override.isHidden ?: team.isHidden,
                isOutOfContest = override.isOutOfContest ?: team.isOutOfContest,
                organizationId = override.organizationId ?: team.organizationId,
                color = override.color ?: team.color,
            )
        }
        return info.copy(teamList = newTeams).autoCreateGroupsAndOrgs()
    }

    /**
     * An override for a single team
     *
     * All fields can be null, existing values are not changed in that case.
     *
     * @param fullName Full name of the team. Will be mostly shown on admin pages.
     * @param displayName Name of the team shown in most places.
     * @param organizationId The id of organization team comes from
     * @param hashTag Team hashtag. Can be shown on some team related pages
     * @param color A color associated with a team.
     * @param isHidden If set to true, the team would be totally hidden.
     * @param isOutOfContest If set to true, the team would not receive rank in scoreboard, but it's submission would still be shown.
     * @param groups The list of the groups team belongs too.
     * @param extraGroups The list of the groups to add to team.
     * @param customFields Map of custom values. They can be used in substitutions in templates.
     * @param medias Map of urls to team related medias. E.g., team photo or some kind of video from workstation.
     *               If media is explicitly set to null, it would be removed if received from a contest system.
     */
    @Serializable
    public class Override(
        public val fullName: String? = null,
        public val displayName: String? = null,
        public val organizationId: OrganizationId? = null,
        public val hashTag: String? = null,
        public val color: Color? = null,
        public val isHidden: Boolean? = null,
        public val isOutOfContest: Boolean? = null,
        public val groups: List<GroupId>? = null,
        public val extraGroups: List<GroupId>? = null,
        public val customFields: Map<String, String>? = null,
        public val medias: Map<TeamMediaType, MediaType?>? = null,
    )
}