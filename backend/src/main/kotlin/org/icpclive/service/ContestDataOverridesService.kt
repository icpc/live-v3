package org.icpclive.service

import fileChangesFlow
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromStream
import org.icpclive.api.AdvancedProperties
import org.icpclive.api.ContestInfo
import org.icpclive.config.Config
import org.icpclive.data.DataBus
import org.icpclive.utils.catchToNull
import org.icpclive.utils.getLogger
import org.icpclive.utils.guessDatetimeFormat
import org.icpclive.utils.humanReadable
import kotlin.io.path.inputStream

class ContestDataOverridesService(val contestInfoInputFlow: StateFlow<ContestInfo>) {
    private val outputFlow = MutableStateFlow(contestInfoInputFlow.value).also {
        DataBus.contestInfoUpdates.complete(it)
    }
    suspend fun run() {
        val advancedPropsFlow = CoroutineScope(Dispatchers.IO).let { scope ->
            fileChangesFlow(Config.configDirectory.resolve("advanced.json")).mapNotNull { path ->
                logger.info("Reloading $path")
                try {
                    path.inputStream().use {
                        Json.decodeFromStream<AdvancedProperties>(it)
                    }.also { logger.info("Successfully reloaded $path") }
                } catch (e: Exception) {
                    logger.error("Failed to reload $path", e)
                    null
                }
            }.stateIn(scope)
        }

        merge(contestInfoInputFlow, advancedPropsFlow).collect {
            val overrides = advancedPropsFlow.value
            val info = contestInfoInputFlow.value
            val teamInfos = if (overrides.teamNamesOverrides == null) {
                info.teams
            } else {
                val done = mutableSetOf<String>()
                info.teams.map {
                    if (it.alias in overrides.teamNamesOverrides) {
                        done.add(it.alias!!)
                        it.copy(shortName = overrides.teamNamesOverrides[it.alias])
                    } else {
                        it
                    }
                }.also {
                    for (alias in overrides.teamNamesOverrides.keys) {
                        if (alias !in done) {
                            logger.warn("No team ${alias} found for override")
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