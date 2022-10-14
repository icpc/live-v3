package org.icpclive.service.social

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch
import org.icpclive.api.SocialEvent
import org.icpclive.config
import org.icpclive.getCredentials
import org.icpclive.service.social.twitch.TwitchService
import java.io.FileInputStream
import java.nio.file.Files
import java.util.*

fun CoroutineScope.launchSocialServices() {
    val path = config.configDirectory.resolve("social.properties")
    if (!Files.exists(path)) return
    val properties = Properties()
    FileInputStream(path.toString()).use { properties.load(it) }
    val rawEvents = MutableSharedFlow<SocialEvent>(
        replay = 500,
        extraBufferCapacity = 0,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    properties.getCredentials("twitch.chat.username")?.let { twitchUsername ->
        val twitchPassword = properties.getCredentials("twitch.chat.token") ?: throw IllegalStateException("Twitch token is not defined")
        val twitchChannels = properties.getProperty("twitch.chat.channel") ?: throw IllegalStateException("Twitch channels is not defined")
        launch {
            TwitchService(
                twitchChannels.split(";"),
                twitchUsername,
                "oauth:$twitchPassword",
            ).run(rawEvents)
        }
    }

    launch { PopulateSocialEventsService().run(rawEvents) }
}
