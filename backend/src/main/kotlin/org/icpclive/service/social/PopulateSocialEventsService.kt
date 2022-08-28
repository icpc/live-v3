package org.icpclive.service.social

import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.*
import org.icpclive.api.ChatMessage
import org.icpclive.api.SocialEvent
import org.icpclive.data.DataBus
import org.icpclive.utils.completeOrThrow

class PopulateSocialEventsService {
    suspend fun run(flow: Flow<SocialEvent>) {
        val outFlow = MutableSharedFlow<SocialEvent>(
            extraBufferCapacity = 500,
            onBufferOverflow = BufferOverflow.DROP_OLDEST
        ).also {
            DataBus.socialEvents.completeOrThrow(it)
        }
        val hashTagsFlow = DataBus.contestInfoFlow.await().map { info ->
            buildMap {
                for (team in info.teams) {
                    if (team.hashTag != null) {
                        put(team.hashTag, team.id)
                    }
                }
            }
        }.distinctUntilChanged()

        combine(flow, hashTagsFlow, ::Pair).collect { (event, hashTags) ->
            outFlow.emit(
                when (event) {
                    is ChatMessage -> event.copy(teamIds = hashTags.entries.filter { event.rawText.contains(it.key) }.map { it.value })
                }
            )
        }
    }
}