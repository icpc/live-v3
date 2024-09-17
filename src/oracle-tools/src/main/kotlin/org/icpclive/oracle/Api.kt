package org.icpclive.oracle

import kotlinx.serialization.Serializable

@Serializable
data class OracleRequest(val oracleId: Int, val teamId: String, val minRadius: Int? = null)

@Serializable
data class TeamLocatorCircleSettings(
    val x: Int,
    val y: Int,
    val radius: Int,
    val teamId: String,
)

@Serializable
data class TeamLocatorSettings(
    val circles: List<TeamLocatorCircleSettings> = emptyList(),
    val scene: String = "default", // FIXME: feature for multi vmix sources coordination. Should be moved to the Widget class
)
