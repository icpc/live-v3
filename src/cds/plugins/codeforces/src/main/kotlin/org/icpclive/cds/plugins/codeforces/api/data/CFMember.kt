package org.icpclive.cds.plugins.codeforces.api.data

import kotlinx.serialization.Serializable

@Serializable
internal data class CFMember(
    val handle: String,
    val name: String? = null,
)