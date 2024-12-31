package org.icpclive.cds.tunning

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.icpclive.cds.api.*

/**
 * A rule, adding a group with given [id] to all teams with ids in the list.
 *
 * If no group with given [id] exists, it would be created, using [id] as
 * both [GroupInfo.id] and [GroupInfo.displayName]. It can be overridden with
 * further [OverrideGroups] rule.
 */
@Serializable
@SerialName("addGroupToTeams")
public data class AddGroupToTeams(
    public val id: GroupId,
    public val teams: List<TeamId>,
): SimpleDesugarable, TuningRule {
    override fun process(info: ContestInfo, submittedTeams: Set<TeamId>): ContestInfo {
        return desugar().process(info, submittedTeams)
    }
    @OptIn(InefficientContestInfoApi::class)
    override fun desugar(): TuningRule {
        return OverrideTeams(
            teams.associateWith { OverrideTeams.Override(extraGroups = listOf(id)) }
        )
    }
}