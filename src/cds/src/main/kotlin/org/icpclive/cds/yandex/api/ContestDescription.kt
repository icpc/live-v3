package org.icpclive.cds.yandex.api

import kotlinx.serialization.Serializable

@Serializable
internal data class ContestDescription(
    val duration: Long,
    val freezeTime: Long?,
    val name: String,
    val startTime: String,
    val type: String
)
