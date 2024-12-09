package org.icpclive.cds.tunning

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.icpclive.cds.api.*

@Serializable
@SerialName("set_organization_by_regex")
public data class SetOrganizationByRegex(
    public val from: String,
    public val rules: RegexSet
): DesugarableTuningRule {
    @OptIn(InefficientContestInfoApi::class)
    override fun desugar(info: ContestInfo): TuningRule {
        return OverrideTeams(
            info.teamList.mapNotNull {
                val fromValue = info.getTemplateValue(from, it.id, isUrl = false)
                val value = rules.applyTo(fromValue) ?: return@mapNotNull null
                it.id to TeamInfoOverride(
                    organizationId = value.toOrganizationId()
                )
            }.toMap()
        )
    }
}