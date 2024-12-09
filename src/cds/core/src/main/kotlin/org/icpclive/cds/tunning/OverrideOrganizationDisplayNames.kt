package org.icpclive.cds.tunning

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.icpclive.cds.api.ContestInfo
import org.icpclive.cds.api.OrganizationId

@Serializable
@SerialName("override_organization_display_names")
public data class OverrideOrganizationDisplayNames(
    val rules: Map<OrganizationId, String>
): DesugarableTuningRule {
    override fun desugar(info: ContestInfo): TuningRule {
        return OverrideOrganizations(rules.mapValues { OrganizationInfoOverride(displayName = it.value) })
    }
}