package org.icpclive.cds.yandex.api

import kotlinx.serialization.Serializable
import org.icpclive.cds.yandex.YandexTeamInfo

@Serializable
data class Participant(
    val id: Int,
    val name: String,
    val login: String,
    val uid: String?
) {
    fun toTeamInfo() = YandexTeamInfo(
        id,
        login,
        name
    )
}