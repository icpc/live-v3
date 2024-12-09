package org.icpclive.cds.tunning

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.icpclive.cds.api.*

@Serializable
@SerialName("addGroupToTeams")
public data class AddGroupToTeams(
    public val id: GroupId,
    public val teams: List<TeamId>,
): SimpleDesugarableTuningRule {
    @OptIn(InefficientContestInfoApi::class)
    override fun desugar(): TuningRule {
        return OverrideTeams(
            teams.associateWith { TeamInfoOverride(extraGroups = listOf(id)) }
        )
    }
}