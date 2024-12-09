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
        return OverrideTeams(
            info.teamList.mapNotNull {
                val fromValue = info.getTemplateValue(from, it.id, isUrl = false)
                if (!rule.matches(fromValue)) return@mapNotNull null
                it.id to TeamInfoOverride(extraGroups = listOf(id))
            }.toMap()
        )
    }
}