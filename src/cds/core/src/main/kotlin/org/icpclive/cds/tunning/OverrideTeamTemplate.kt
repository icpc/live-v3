package org.icpclive.cds.tunning

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.icpclive.cds.api.*

@Serializable
@SerialName("override_team_template")
public data class OverrideTeamTemplate(
    public val displayName: String? = null,
    public val fullName: String? = null,
    public val hashTag: String? = null,
    public val medias: Map<TeamMediaType, MediaType?>? = null,
    public val color: String? = null,
): DesugarableTuningRule {
    @OptIn(InefficientContestInfoApi::class)
    override fun desugar(info: ContestInfo): TuningRule {
        return OverrideTeams(
            info.teamList.associate { teamInfo ->
                fun String.substituted(isUrl: Boolean) = info.getTemplateValue(this, teamInfo.id, isUrl)
                teamInfo.id to TeamInfoOverride(
                    hashTag = hashTag?.substituted(false),
                    fullName = fullName?.substituted(false),
                    displayName = displayName?.substituted(false),
                    medias = medias?.mapValues { (_, v) ->
                        v?.let {
                            when (it) {
                                is MediaType.Image -> it.copy(url = it.url.substituted(true))
                                is MediaType.Video -> it.copy(url = it.url.substituted(true))
                                is MediaType.M2tsVideo -> it.copy(url = it.url.substituted(true))
                                is MediaType.HLSVideo -> it.copy(url = it.url.substituted(true))
                                is MediaType.Object -> it.copy(url = it.url.substituted(true))
                                is MediaType.WebRTCProxyConnection -> it.copy(url = it.url.substituted(true))
                                is MediaType.WebRTCGrabberConnection -> it.copy(
                                    url = it.url.substituted(true),
                                    peerName = it.peerName.substituted(false),
                                    credential = it.credential?.substituted(false)
                                )

                                else -> it
                            }
                        }
                    },
                    color = color?.substituted(false)?.let { Color.normalize(it) }
                )
            }
        )
    }
}