package org.icpclive.service

import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.icpclive.api.*
import org.icpclive.utils.catchToNull
import org.icpclive.utils.getLogger
import org.icpclive.utils.guessDatetimeFormat
import org.icpclive.utils.humanReadable
import kotlin.time.Duration.Companion.seconds

class ContestDataPostprocessingService {
    private fun <T, O> mergeOverride(
        infos: List<T>,
        overrides: Map<String, O>?,
        id: T.() -> String,
        getTemplate: ((String) -> O?) = { null },
        merge: (T, O) -> T
    ): Pair<List<T>, List<String>> {
        return if (overrides == null)
            infos to emptyList()
        else {
            val done = mutableSetOf<String>()
            infos.map { info ->
                (overrides[info.id()] ?: getTemplate(info.id()))?.let {
                    done.add(info.id())
                    merge(info, it)
                } ?: info
            } to overrides.keys.filter { it !in done }
        }
    }

    suspend fun run(
        contestInfoInputFlow: Flow<ContestInfo>,
        advancedPropsFlow: Flow<AdvancedProperties>,
        rawRunsFlow: Flow<RunInfo>,
        outputFlow: MutableStateFlow<ContestInfo>
    ) {
        coroutineScope {
            val mutex = Mutex()
            val submittedTeams = mutableSetOf<Int>()
            val teamsCountFlow = MutableStateFlow(0)
            launch {
                rawRunsFlow.map { it.teamId }.collect {
                    if (!submittedTeams.contains(it)) {
                        mutex.withLock {
                            submittedTeams.add(it)
                            teamsCountFlow.value = submittedTeams.size
                        }
                    }
                }
            }
            combine(contestInfoInputFlow, advancedPropsFlow, teamsCountFlow, ::Triple).collect { (info, overrides, _) ->
                val (teamInfos, unusedTeamOverrides) = mergeOverride(
                    info.teams,
                    overrides.teamOverrides,
                    TeamInfo::contestSystemId,
                    { id ->
                        overrides.teamOverrideTemplate?.let { override ->
                            override.copy(medias = override.medias?.mapValues { it.value?.replace("{teamId}", id) })
                        }
                    }
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
                        ProblemInfo.parseColor(override.color) ?: problem.color,
                        problem.id,
                        problem.ordinal
                    )
                }
                val startTime = overrides.startTime
                    ?.let { catchToNull { guessDatetimeFormat(it) } }
                    ?.also { logger.info("Contest start time overridden to ${it.humanReadable}") }
                    ?: info.startTime
                val holdTimeSeconds = if (overrides.holdTimeSeconds == "null")
                    null
                else
                    overrides.holdTimeSeconds?.toIntOrNull()?.seconds ?: info.holdBeforeStartTime
                val medals = overrides.scoreboardOverrides?.medals ?: info.medals
                val penaltyPerWrongAttempt =
                    overrides.scoreboardOverrides?.penaltyPerWrongAttempt ?: info.penaltyPerWrongAttempt
                if (unusedTeamOverrides.isNotEmpty()) logger.warn("No team for override: $unusedTeamOverrides")
                if (unusedProblemOverrides.isNotEmpty()) logger.warn("No problem for override: $unusedProblemOverrides")

                val teamInfosFiltered = if (overrides.scoreboardOverrides?.showTeamsWithoutSubmissions != false) {
                    teamInfos
                } else {
                    mutex.withLock {
                        teamInfos.filter { it.id in submittedTeams }
                    }
                }

                logger.info("Team and problem overrides are reloaded")
                outputFlow.value = info.copy(
                    startTime = startTime,
                    holdBeforeStartTime = holdTimeSeconds,
                    teams = teamInfosFiltered,
                    problems = problemInfos.sortedBy { it.ordinal },
                    medals = medals,
                    penaltyPerWrongAttempt = penaltyPerWrongAttempt
                )
            }
        }
    }

    companion object {
        val logger = getLogger(ContestDataPostprocessingService::class)
    }
}
