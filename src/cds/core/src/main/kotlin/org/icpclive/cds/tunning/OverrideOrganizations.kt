package org.icpclive.cds.tunning

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.icpclive.cds.api.*
import org.icpclive.cds.util.getLogger

/**
 * A rule overriding settings of each organization separately.
 * If there is override for a non-existent organization, a warning is issued.
 * If there is no override for an organization, values from the contest system are used.
 *
 * @param rules a map from organization id to [Override] for this organization. Check [Override] doc for details.
 */
@Serializable
@SerialName("overrideOrganizations")
public data class OverrideOrganizations(
    public val rules: Map<OrganizationId, Override>
): TuningRule {
    @OptIn(InefficientContestInfoApi::class)
    override fun process(info: ContestInfo): ContestInfo {
        return info.copy(
            organizationList = mergeOverrides(
                info.organizationList,
                rules,
                { id },
                logUnused = { logger.warning { "No organization for override: $it" } }
            ) { org, override ->
                org.copy(
                    displayName = override.displayName ?: org.displayName,
                    fullName = override.fullName ?: org.fullName,
                    logo = override.logo ?: org.logo
                )
            }
        )
    }

    /**
     * An override for a single organization
     *
     * All fields can be null, existing values are not changed in that case.
     *
     * @param displayName Name of the organization shown in most places.
     * @param fullName Full name of the organization. Will be mostly shown on admin pages.
     * @param logo Organization logo. Not displayed anywhere for now, but can be exported to e.g., icpc resolved.
     */
    @Serializable
    public class Override(
        public val displayName: String? = null,
        public val fullName: String? = null,
        public val logo: MediaType? = null,
    )

    private companion object {
        val logger by getLogger()
    }
}