package org.icpclive.api

import kotlinx.serialization.Serializable

@Serializable
public data class TeamKeylog(
    val intervalMs: Long,
    val values: List<Double>,
)