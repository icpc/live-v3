package org.icpclive.sniper

import kotlinx.serialization.Serializable

@Serializable
data class MoveSniperConfig(val sniperNumber: Int, val teamId: Int)

@Serializable
data class ShowLocatorConfig(val sniperNumber: Int, val teamIds: List<Int>)
