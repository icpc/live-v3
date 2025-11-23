package org.icpclive.cds.api

import kotlinx.serialization.Required
import kotlinx.serialization.Serializable

@JvmInline
@Serializable
public value class OrganizationId internal constructor(public val value: String) {
    override fun toString(): String = value
}

public fun String.toOrganizationId(): OrganizationId = OrganizationId(this)
public fun Int.toOrganizationId(): OrganizationId = toString().toOrganizationId()
public fun Long.toOrganizationId(): OrganizationId = toString().toOrganizationId()


@Serializable
public data class OrganizationInfo(
    val id: OrganizationId,
    val displayName: String,
    val fullName: String,
    val logo: List<MediaType>,
    val country: String? = null,
    val countryFlag: List<MediaType> = emptyList(),
    val countrySubdivision: String? = null,
    val countrySubdivisionFlag: List<MediaType> = emptyList(),
    @Required val customFields: Map<String, String> = emptyMap()
)