package org.icpclive.api

import kotlinx.serialization.Serializable

@Serializable
sealed class SocialEvent {
    abstract val rawText: String
    abstract val teamIds: List<Int>
    abstract val author: String
}

@Serializable
data class ChatMessage(
    override val rawText: String,
    override val teamIds: List<Int>,
    override val author: String,
    val platform: String,
) : SocialEvent()