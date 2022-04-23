package org.icpclive.cds.yandex

import org.icpclive.api.MediaType
import org.icpclive.api.RunInfo
import org.icpclive.cds.TeamInfo

class YandexTeamInfo(
    override val id: Int,
    override val contestSystemId: String,
    override val name: String
) : TeamInfo {
    override val shortName: String = name.split("(").dropLast(1).joinToString("(")
    override val groups = emptySet<String>()
    override val hashTag: String? = null
    override val medias = emptyMap<MediaType, String>()
}