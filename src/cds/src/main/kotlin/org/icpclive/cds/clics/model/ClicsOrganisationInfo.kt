package org.icpclive.cds.clics.model

import org.icpclive.api.MediaType

internal data class ClicsOrganisationInfo(
    val id: String,
    val name: String,
    val formalName: String,
    val logo: MediaType?,
    val hashtag: String?
)
