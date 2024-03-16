package org.icpclive.cds.api

import kotlinx.serialization.Serializable

@JvmInline
@Serializable
public value class OrganizationId(public val value: String) {
    override fun toString(): String = value
}

@Serializable
public data class OrganizationInfo(
    val id: OrganizationId,
    val displayName: String,
    val fullName: String,
    val logo: MediaType?,
)