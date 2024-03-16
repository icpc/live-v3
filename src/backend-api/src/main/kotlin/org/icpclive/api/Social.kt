package org.icpclive.api

import kotlinx.serialization.Serializable
import org.icpclive.cds.api.TeamId

@Serializable
sealed class SocialEvent {
    abstract val rawText: String
    abstract val teamIds: List<TeamId>
    abstract val author: String
}

@Serializable
data class ChatMessage(
    override val rawText: String,
    override val teamIds: List<TeamId> = emptyList(),
    override val author: String,
    val platform: String,
) : SocialEvent()