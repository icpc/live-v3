package org.icpclive.service

import fileChangesFlow
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromStream
import org.icpclive.api.AdvancedProperties
import org.icpclive.api.ContestInfo
import org.icpclive.api.ProblemInfo
import org.icpclive.api.TeamInfo
import org.icpclive.config.Config
import org.icpclive.data.DataBus
import org.icpclive.utils.*
import kotlin.io.path.inputStream

class ContestDataOverridesService(private val contestInfoInputFlow: StateFlow<ContestInfo>) {
    private val outputFlow = MutableStateFlow(contestInfoInputFlow.value).also {
        DataBus.contestInfoUpdates.completeOrThrow(it)
    }

    private fun <T, O> mergeOverride(infos: List<T>, overrides: Map<String, O>?, id: T.() -> String, merge: (T, O) -> T) : Pair<List<T>, List<String>> {
        return if (overrides == null)
            infos to emptyList()
        else {
            val done = mutableSetOf<String>()
            infos.map { info ->
                overrides[info.id()]?.let {
                    done.add(info.id())
                    merge(info, it)
                } ?: info
            } to overrides.keys.filter { it !in done }
        }
    }

    suspend fun run() {
        val advancedPropsFlow = CoroutineScope(Dispatchers.IO).let { scope ->
            val flow = fileChangesFlow(Config.configDirectory.resolve("advanced.json")).mapNotNull { path ->
                logger.info("Reloading $path")
                try {
                    path.inputStream().use {
                        Json.decodeFromStream<AdvancedProperties>(it)
                    }
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
            val (teamInfos, unusedTeamOverrides) = mergeOverride(info.teams, overrides.teamOverrides, TeamInfo::contestSystemId) { team, override ->
                TeamInfo(
                    team.id,
                    override.name ?: team.name,
                    override.shortname ?: team.shortName,
                    team.contestSystemId,
                    override.groups ?: team.groups,
                    override.hashTag ?: team.hashTag,
                    if (override.medias != null)
                        (team.medias + override.medias).filterValues { it != null }.mapValues { it.value!! }
                    else
                        team.medias
                )
            }
            val (problemInfos, unusedProblemOverrides) = mergeOverride(info.problems, overrides.problemOverrides, ProblemInfo::letter) { problem, override ->
                ProblemInfo(
                    problem.letter,
                    override.name ?: problem.name,
                    override.color ?: problem.color
                )
            }
            val startTime = overrides.startTime
                ?.let { catchToNull { guessDatetimeFormat(it) } }
                ?.also { logger.info("Contest start time overridden to ${it.humanReadable}") }
                ?.toEpochMilliseconds()
                ?: info.startTimeUnixMs
            outputFlow.value = info.copy(
                startTimeUnixMs = startTime,
                teams = teamInfos,
                problems = problemInfos
            )
            if (unusedTeamOverrides.isNotEmpty()) logger.warn("No team for override: $unusedTeamOverrides")
            if (unusedProblemOverrides.isNotEmpty()) logger.warn("No problem for override: $unusedProblemOverrides")
            logger.info("Team and problem overrides are reloaded")
        }
    }
    companion object {
        val logger = getLogger(ContestDataOverridesService::class)
    }
}