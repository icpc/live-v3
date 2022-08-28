package org.icpclive.service.social

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch
import org.icpclive.api.SocialEvent
import org.icpclive.config
import org.icpclive.service.social.twitch.TwitchService
import org.icpclive.utils.processCreds
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
    properties.getProperty("twitch.chat.username").takeIf { it.isNotEmpty() }?.let { twitchUsername ->
        val twitchPassword = properties.getProperty("twitch.chat.token")!!
        val twitchChannels = properties.getProperty("twitch.chat.channel")!!
        launch {
            TwitchService(
                twitchChannels.split(";"),
                twitchUsername.processCreds(),
                "oauth:" + twitchPassword.processCreds(),
            ).run(rawEvents)
        }
    }

    launch { PopulateSocialEventsService().run(rawEvents) }
}
