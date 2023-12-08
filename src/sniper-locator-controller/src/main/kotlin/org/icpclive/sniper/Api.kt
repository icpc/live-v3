package org.icpclive.sniper

import kotlinx.serialization.Serializable

@Serializable
data class Api(val sniperID: Int, val teamID: Int)
