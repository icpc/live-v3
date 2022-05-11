package org.icpclive.cds.clics.model

import org.icpclive.api.MediaType
import org.icpclive.cds.TeamInfo

data class ClicsTeamInfo(
    override val id: Int,
    override val name: String,
    override val shortName: String,
    override val contestSystemId: String,
    override val groups: Set<String>,
    override val hashTag: String?,
    val photo: String? = null,
    val video: String? = null,
    val screens: List<String>,
    val cameras: List<String>,
) : TeamInfo {
    override val medias: Map<MediaType, String> by lazy {
        val medias = mutableMapOf<MediaType, String>()
        video?.let { medias[MediaType.RECORD] = it }
        cameras.getOrNull(0)?.let { medias[MediaType.CAMERA] = it }
        screens.getOrNull(0)?.let { medias[MediaType.SCREEN] = it }
        medias
    }
}
