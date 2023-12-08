package org.icpclive.sniper

import kotlinx.serialization.Serializable

@Serializable
data class MoveRequest(val sniperID: Int, val teamID: Int)