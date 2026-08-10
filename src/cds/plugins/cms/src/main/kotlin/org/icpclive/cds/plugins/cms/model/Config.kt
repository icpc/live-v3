package org.icpclive.cds.plugins.cms.model

import kotlinx.serialization.Serializable

@Serializable
public class Config(
    public val faces_extension: String,
    public val flags_extension: String
)
