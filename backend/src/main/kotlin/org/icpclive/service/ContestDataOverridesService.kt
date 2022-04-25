package org.icpclive.service

import fileChangesFlow
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromStream
import org.icpclive.api.AdvancedProperties
import org.icpclive.api.ContestInfo
import org.icpclive.api.TeamInfo
import org.icpclive.config.Config
import org.icpclive.data.DataBus
import org.icpclive.utils.*
import kotlin.io.path.inputStream

class ContestDataOverridesService(val contestInfoInputFlow: StateFlow<ContestInfo>) {
    private val outputFlow = MutableStateFlow(contestInfoInputFlow.value).also {
        DataBus.contestInfoUpdates.completeOrThrow(it)
    }
    suspend fun run() {
        val advancedPropsFlow = CoroutineScope(Dispatchers.IO).let { scope ->
            val flow = fileChangesFlow(Config.configDirectory.resolve("advanced.json")).mapNotNull { path ->
                logger.info("Reloading $path")
                try {
                    path.inputStream().use {
                        Json.decodeFromStream<AdvancedProperties>(it)
                    }.also { logger.info("Successfully reloaded $path") }
                } catch (e: Exception) {
                    logger.error("Failed to reload $path", e)
                    null
                }
            }
            flow.onStart { emit(AdvancedProperties(null, null)) }
                .stateIn(scope)
                .also { DataBus.advancedPropertiesFlow.completeOrThrow(it) }
        }

        merge(contestInfoInputFlow, advancedPropsFlow).collect {
            val overrides = advancedPropsFlow.value
            val info = contestInfoInputFlow.value
            val teamInfos = if (overrides.teamOverrides == null) {
                info.teams
            } else {
                val done = mutableSetOf<String>()
                info.teams.map {
                    val override = overrides.teamOverrides[it.contestSystemId] ?: return@map it
                    TeamInfo(
                        it.id,
                        override.name ?: it.name,
                        override.shortname ?: it.shortName,
                        it.contestSystemId,
                        override.groups ?: it.groups,
                        override.hashTag ?: it.hashTag,
                        if (override.medias != null) (it.medias + override.medias).filterValues { it != null }.mapValues { it.value!! } else it.medias
                    )
                }.also {
                    for (alias in overrides.teamOverrides.keys) {
                        if (alias !in done) {
                            logger.warn("No team $alias found for override")
                        }
                    }
                }
            }
            val startTime = overrides.startTime
                ?.let { catchToNull { guessDatetimeFormat(it) } }
                ?.also { logger.info("Contest start time overridden to ${it.humanReadable}") }
                ?.toEpochMilliseconds()
                ?: info.startTimeUnixMs
            outputFlow.value = info.copy(
                startTimeUnixMs = startTime,
                teams = teamInfos
            )
        }
    }
    companion object {
        val logger = getLogger(ContestDataOverridesService::class)
    }
}