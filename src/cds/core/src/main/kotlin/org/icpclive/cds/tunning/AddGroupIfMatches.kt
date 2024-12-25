package org.icpclive.cds.tunning

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.icpclive.cds.api.*

@Serializable
@SerialName("add_group_if_matches")
public data class AddGroupIfMatches(
    public val id: GroupId,
    public val from: String,
    public val rule: Regex
): DesugarableTuningRule {
    @OptIn(InefficientContestInfoApi::class)
    override fun desugar(info: ContestInfo): TuningRule {
        return AddGroupToTeams(
            id,
            info.teamList.mapNotNull {
                val fromValue = info.getTemplateValue(from, it.id, isUrl = false)
                it.id.takeIf { rule.matches(fromValue) }
            }
        )
    }
}

@Serializable
@SerialName("add_group_to_teams")
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