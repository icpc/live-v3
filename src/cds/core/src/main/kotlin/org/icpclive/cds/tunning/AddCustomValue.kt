package org.icpclive.cds.tunning

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.icpclive.cds.api.*

@Serializable
@SerialName("add_custom_value")
public data class AddCustomValue(
    public val name: String,
    public val rules: Map<TeamId, String>
): TuningRule {
    @OptIn(InefficientContestInfoApi::class)
    override fun process(info: ContestInfo, submittedTeams: Set<TeamId>): ContestInfo {
        return info.copy(
            teamList = info.teamList.map {
                val s = rules[it.id] ?: return@map it
                it.copy(customFields = it.customFields + (name to s))
            }
        )
    }
}