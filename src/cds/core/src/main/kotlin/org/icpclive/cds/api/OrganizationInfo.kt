package org.icpclive.cds.api

import kotlinx.serialization.Serializable

@Serializable
public data class OrganizationInfo(
    val cdsId: String,
    val displayName: String,
    val fullName: String,
    val logo: MediaType?,
)