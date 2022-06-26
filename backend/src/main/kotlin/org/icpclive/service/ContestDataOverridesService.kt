package org.icpclive.service

import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.*
import org.icpclive.api.AdvancedProperties
import org.icpclive.api.ContestInfo
import org.icpclive.api.ProblemInfo
import org.icpclive.api.TeamInfo
import org.icpclive.utils.*

class ContestDataOverridesService(
    ) {
    private fun <T, O> mergeOverride(
        infos: List<T>,
        overrides: Map<String, O>?,
        id: T.() -> String,
        merge: (T, O) -> T
    ): Pair<List<T>, List<String>> {
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

    suspend fun run(
        contestInfoInputFlow: Flow<ContestInfo>,
        advancedPropsFlow: Flow<AdvancedProperties>,
        outputFlow: MutableStateFlow<ContestInfo>
    ) {
        coroutineScope {
            combine(contestInfoInputFlow, advancedPropsFlow) { info, overrides ->
                val (teamInfos, unusedTeamOverrides) = mergeOverride(
                    info.teams,
                    overrides.teamOverrides,
                    TeamInfo::contestSystemId
                ) { team, override ->
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
                val (problemInfos, unusedProblemOverrides) = mergeOverride(
                    info.problems,
                    overrides.problemOverrides,
                    ProblemInfo::letter
                ) { problem, override ->
                    ProblemInfo(
                        problem.letter,
                        override.name ?: problem.name,
                        ProblemInfo.parseColor(override.color) ?: problem.color
                    )
                }
                val startTime = overrides.startTime
                    ?.let { catchToNull { guessDatetimeFormat(it) } }
                    ?.also { logger.info("Contest start time overridden to ${it.humanReadable}") }
                    ?: info.startTime
                if (unusedTeamOverrides.isNotEmpty()) logger.warn("No team for override: $unusedTeamOverrides")
                if (unusedProblemOverrides.isNotEmpty()) logger.warn("No problem for override: $unusedProblemOverrides")
                logger.info("Team and problem overrides are reloaded")
                outputFlow.value = info.copy(
                    startTime = startTime,
                    teams = teamInfos,
                    problems = problemInfos
                )
            }}
    }

    companion object {
        val logger = getLogger(ContestDataOverridesService::class)
    }
}