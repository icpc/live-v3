package org.icpclive.cds.tunning

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.icpclive.cds.api.ContestInfo
import org.icpclive.cds.api.TeamId

@Serializable
@SerialName("override_team_display_names")
public data class OverrideTeamDisplayNames(
    val rules: Map<TeamId, String>
): SimpleDesugarableTuningRule {
    override fun desugar(): TuningRule {
        return OverrideTeams(rules.mapValues { TeamInfoOverride(displayName = it.value) })
    }
}