package org.icpclive.cds.plugins.cms.model

import kotlinx.serialization.Serializable

@Serializable
internal class User(
    val f_name: String,
    val l_name: String,
    val team: String,
)