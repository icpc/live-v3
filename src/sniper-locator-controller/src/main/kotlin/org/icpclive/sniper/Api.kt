package org.icpclive.sniper

import kotlinx.serialization.Serializable

@Serializable
data class SniperRequest(val sniperId: Int, val teamId: Int)

@Serializable
data class TeamLocatorCircleSettings(
    val x: Int,
    val y: Int,
    val radius: Int,
    val teamId: Int,
)

@Serializable
data class TeamLocatorSettings(
    val circles: List<TeamLocatorCircleSettings> = emptyList(),
    val scene: String = "default", // FIXME: feature for multi vmix sources coordination. Should be moved to the Widget class
)
