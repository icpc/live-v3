package org.icpclive.cds.yandex

import org.icpclive.api.MediaType
import org.icpclive.cds.TeamInfo

class YandexTeamInfo(
    override val id: Int,
    override val contestSystemId: String,
    override val name: String
) : TeamInfo {
    override val shortName: String = formatShortName(name)
    override val groups = emptySet<String>()
    override val hashTag: String? = null
    override val medias = emptyMap<MediaType, String>()

    companion object {
        fun formatShortName(name: String): String {
            if ("(" !in name) {
                return name
            } else {
                return name.split("(").dropLast(1).joinToString("(")
            }
        }
    }
}