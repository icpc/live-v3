package org.icpclive.cds.tunning

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.icpclive.cds.api.*
import org.icpclive.cds.util.ListOrSingleElementSerializer
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
                    logo = (override.logo ?: org.logo) + override.extraLogos.orEmpty(),
                    country = override.country ?: org.country,
                    countryFlag = (override.countryFlag ?: org.countryFlag) + override.extraCountryFlags.orEmpty(),
                    countrySubdivision = override.countrySubdivision ?: org.countrySubdivision,
                    countrySubdivisionFlag = (override.countrySubdivisionFlag ?: org.countrySubdivisionFlag) + override.extraCountrySubdivisionFlags.orEmpty(),
                    customFields = mergeMaps(org.customFields, override.customFields),
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
     * @param logo Organization logo. Replace data from cds
     * @param extraLogos Organization logo. Adds to data from cds
     * @param country ISO 3166-1 alpha-3 code of the organization's country.
     * @param countryFlag Country flags of the organization. Replace data from cds
     * @param extraCountryFlags Country flags of the organization. Adds to data from cds
     * @param countrySubdivision ISO 3166-2 code of country subdivision of the organization (like region or state). Adds to data from cds
     * @param countrySubdivisionFlag Subdivision flags. Replace data from cds
     * @param extraCountrySubdivisionFlags Subdivision flags. Adds to data from cds
     * @param customFields Map of custom values. They can be used in substitutions in templates.
     */
    @Serializable
    public class Override(
        public val displayName: String? = null,
        public val fullName: String? = null,
        @Serializable(with = ListOrSingleElementSerializer::class) public val logo: List<MediaType>? = null,
        @Serializable(with = ListOrSingleElementSerializer::class) public val extraLogos: List<MediaType>? = null,
        public val country: String? = null,
        @Serializable(with = ListOrSingleElementSerializer::class) public val countryFlag: List<MediaType>? = null,
        @Serializable(with = ListOrSingleElementSerializer::class) public val extraCountryFlags: List<MediaType>? = null,
        public val countrySubdivision: String? = null,
        @Serializable(with = ListOrSingleElementSerializer::class) public val countrySubdivisionFlag: List<MediaType>? = null,
        @Serializable(with = ListOrSingleElementSerializer::class) public val extraCountrySubdivisionFlags: List<MediaType>? = null,
        public val customFields: Map<String, String>? = null,
    )

    private companion object {
        val logger by getLogger()
    }
}