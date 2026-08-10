package org.icpclive.cds.plugins.cms.model

import kotlinx.serialization.Serializable

@Serializable
internal class Config(
    val faces_extension: String,
    val flags_extension: String
)
