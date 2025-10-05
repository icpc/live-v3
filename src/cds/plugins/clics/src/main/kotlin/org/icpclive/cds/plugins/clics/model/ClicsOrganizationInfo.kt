package org.icpclive.cds.plugins.clics.model

import org.icpclive.cds.api.MediaType

internal data class ClicsOrganizationInfo(
    val id: String,
    val name: String,
    val formalName: String,
    val logo: List<MediaType>,
    val hashtag: String?,
    val country: String?,
)
