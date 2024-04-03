package org.icpclive.oracle

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromStream
import java.util.*
import kotlin.io.path.exists

@Serializable
data class SniperRequest(val sniperId: Int, val teamId: String)

@Serializable
data class TeamLocatorCircleSettings(
    val x: Int,
    val y: Int,
    val radius: Int,
    val cdsTeamId: String,
)

@Serializable
data class TeamLocatorSettings(
    val circles: List<TeamLocatorCircleSettings> = emptyList(),
    val scene: String = "default", // FIXME: feature for multi vmix sources coordination. Should be moved to the Widget class
)
