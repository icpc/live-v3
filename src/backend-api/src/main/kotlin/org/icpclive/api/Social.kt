package org.icpclive.api

import kotlinx.serialization.SerialName
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
    @SerialName("chatmessage")
    override val rawText: String,

    override val teamIds: List<TeamId> = emptyList(),

    @SerialName("chatname")
    override val author: String,

    @SerialName("type")
    val platform: String,
) : SocialEvent()
