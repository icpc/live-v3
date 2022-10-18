package org.icpclive.service.social.twitch


import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import org.icpclive.api.ChatMessage
import org.icpclive.api.SocialEvent
import org.icpclive.util.getLogger
import org.icpclive.util.suppressIfNotCancellation
import kotlin.time.Duration.Companion.seconds

class TwitchService(
    val channels: List<String>,
    val account: String,
    val password: String
) {
    suspend fun run(flow: MutableSharedFlow<SocialEvent>) {
        logger.info("Starting Twitch service for channels $channels")
        while (true) {
            try {
                Client(channels, account, password).run {author, message ->
                    flow.emit(
                        ChatMessage(
                            rawText = message,
                            teamIds = emptyList(),
                            author = author ?: "unknown",
                            platform = "twitch"
                        )
                    )
                }
            } catch (e: Exception) {
                suppressIfNotCancellation(e)
                logger.error("Twitch client failed: restarting", e)
                delay(10.seconds)
            }
        }
    }

    companion object {
        val logger = getLogger(TwitchService::class)
    }
}