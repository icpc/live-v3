package org.icpclive.cds.tunning

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.icpclive.cds.api.*
import org.icpclive.cds.util.getLogger

/**
 * @param displayName Name of the team shown in most places.
 * @param fullName Full name of the organization. Will be mostly shown on admin pages.
 * @param logo Organization logo. Not displayed anywhere for now, but can be exported to e.g., icpc resolved.
 */
@Serializable
public class OrganizationInfoOverride(
    public val displayName: String? = null,
    public val fullName: String? = null,
    public val logo: MediaType? = null,
)

@Serializable
@SerialName("override_organizations")
public data class OverrideOrganizations(
    public val rules: Map<OrganizationId, OrganizationInfoOverride>
): TuningRule {
    @OptIn(InefficientContestInfoApi::class)
    override fun process(info: ContestInfo, submittedTeams: Set<TeamId>): ContestInfo {
        return info.copy(
            organizationList = mergeOverrides(
                info.organizationList,
                rules,
                { id },
                logUnused = { logger.warning { "No organization for override: $it" } }
            ) { org, override ->
                OrganizationInfo(
                    id = org.id,
                    displayName = override.displayName ?: org.displayName,
                    fullName = override.fullName ?: org.fullName,
                    logo = override.logo ?: org.logo
                )
            }
        )
    }

    private companion object {
        val logger by getLogger()
    }
}