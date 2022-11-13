package org.icpclive.cds.codeforces.api.data

import kotlinx.serialization.Serializable

@Serializable
data class CFMember(
    val handle: String,
    val name: String? = null,
)