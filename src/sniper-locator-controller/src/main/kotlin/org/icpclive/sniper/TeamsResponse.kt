package org.icpclive.sniper

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonArray

@Serializable
data class TeamsResponse(val teams: List<TeamInfo>)