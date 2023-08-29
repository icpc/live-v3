package org.icpclive.cds.cms.model

import kotlinx.serialization.Serializable

@Serializable
class User(
    val f_name: String,
    val l_name: String,
    val team: String
)