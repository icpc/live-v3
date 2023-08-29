package org.icpclive.cds.cms.model

import kotlinx.serialization.Serializable

@Serializable
class Subchange(
    val score: Double,
    val submission: String,
    val extra: List<String>,
    val time: Int
)