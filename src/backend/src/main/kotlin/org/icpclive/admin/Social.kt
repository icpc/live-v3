package org.icpclive.admin

import io.ktor.server.application.*
import io.ktor.server.routing.*
import io.ktor.server.websocket.*
import kotlinx.coroutines.flow.MutableSharedFlow
import org.icpclive.api.ChatMessage
import org.icpclive.api.SocialEvent
import org.icpclive.cds.api.TeamId
import org.icpclive.util.completeOrThrow
import org.icpclive.data.DataBus
import org.icpclive.util.sendJsonFlow

fun Route.setupSocial() {
    val socialFlow = MutableSharedFlow<SocialEvent>(extraBufferCapacity = 10000)
    DataBus.socialEvents.completeOrThrow(socialFlow)

    suspend fun processMessage(message: ChatMessage): ChatMessage {
        val newTeamIds =
            message.teamIds + getHashtags().entries.filter { message.rawText.contains(it.key) }.map { it.value }
        return message.copy(teamIds = newTeamIds.distinct())
    }

    webSocket { sendJsonFlow(DataBus.socialEvents.await()) }

    post {
        call.adminApiAction {
            socialFlow.emit(processMessage(call.safeReceive()))
        }
    }
}
