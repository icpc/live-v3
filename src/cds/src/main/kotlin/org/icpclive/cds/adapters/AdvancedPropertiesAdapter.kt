package org.icpclive.cds.adapters

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import org.icpclive.api.*
import org.icpclive.cds.ContestDataSource
import org.icpclive.util.*
import kotlin.time.Duration

internal class AdvancedPropertiesAdapter(
    private val source: ContestDataSource,
    private val advancedPropsFlow: CompletableDeferred<Flow<AdvancedProperties>>
) : ContestDataSource {
    private fun <T, O> mergeOverride(
        infos: List<T>,
        overrides: Map<String, O>?,
        id: T.() -> String,
        getTemplate: ((String) -> O?) = { null },
        merge: (T, O) -> T
    ): Pair<List<T>, List<String>> {
        return if (overrides == null) {
            infos to emptyList()
        } else {
            val idsSet = infos.map { it.id() }.toSet()
            fun mergeIfNotNull(a: T, b: O?) = if (b == null) a else merge(a, b)
            infos.map {
                mergeIfNotNull(
                    mergeIfNotNull(it, getTemplate(it.id())),
                    overrides[it.id()]
                )
            } to overrides.keys.filter { it !in idsSet }
        }
    }

    override suspend fun run(
        contestInfoDeferred: CompletableDeferred<StateFlow<ContestInfo>>,
        runsDeferred: CompletableDeferred<Flow<RunInfo>>,
        analyticsMessagesDeferred: CompletableDeferred<Flow<AnalyticsMessage>>
    ) {
        val unprocessedContestInfo = CompletableDeferred<StateFlow<ContestInfo>>()
        coroutineScope {
            launch {
                source.run(unprocessedContestInfo, runsDeferred, analyticsMessagesDeferred)
            }
            val mutex = Mutex()
            val submittedTeams = mutableSetOf<Int>()
            val forceRecalculationTriggerFlow = MutableStateFlow(0)
            launch {
                runsDeferred.await().map { it.teamId }.collect { teamId ->
                    if (!submittedTeams.contains(teamId)) {
                        mutex.withLock {
                            submittedTeams.add(teamId)
                            forceRecalculationTriggerFlow.update { it + 1 }
                        }
                    }
                }
            }

            launch {
                val timesSet = mutableSetOf<Instant>()
                fun CoroutineScope.triggerAt(time: Instant) {
                    if (time < Clock.System.now()) return
                    if (timesSet.add(time)) {
                        launch {
                            delay(time - Clock.System.now())
                            forceRecalculationTriggerFlow.update { it + 1 }
                        }
                    }
                }
                combine(unprocessedContestInfo.await(), advancedPropsFlow.await(), ::Pair).collect {(info, overrides) ->
                    overrides.startTime
                        ?.let { catchToNull { guessDatetimeFormat(it) } }
                        ?.let {
                            triggerAt(it)
                            triggerAt(it + info.contestLength)
                        }
                }
            }

            contestInfoDeferred.complete(
                combine(
                    unprocessedContestInfo.await(),
                    advancedPropsFlow.await(),
                    forceRecalculationTriggerFlow,
                    ::Triple
                ).map { (info, overrides, _) ->
                    applyOverrides(info, overrides, mutex.withLock { submittedTeams.toSet() })
                }.stateIn(this)
            )
        }
    }

    private fun getStateBasedOnStartTime(time: Instant, contestLength: Duration) : ContestStatus {
        val offset = Clock.System.now() - time
        return when {
            offset < Duration.ZERO -> ContestStatus.BEFORE
            offset < contestLength -> ContestStatus.RUNNING
            else -> ContestStatus.OVER
        }
    }

    private fun applyOverrides(info: ContestInfo, overrides: AdvancedProperties, submittedTeams: Set<Int>) : ContestInfo {
        val (teamInfos, unusedTeamOverrides) = mergeOverride(
            info.teams,
            overrides.teamOverrides,
            TeamInfo::contestSystemId,
            { id ->
                TeamInfoOverride(
                    medias = overrides.teamMediaTemplate?.mapValues {
                        it.value?.applyTemplate(id)
                    }
                )
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
                    team.medias,
                additionalInfo = override.additionalInfo,
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
                problem.id,
                problem.ordinal,
                override.minScore ?: problem.minScore,
                override.maxScore ?: problem.maxScore,
                override.color ?: problem.color,
                override.scoreMergeMode ?: problem.scoreMergeMode
            )
        }
        val (startTime, status) = overrides.startTime
            ?.let { catchToNull { guessDatetimeFormat(it) } }
            ?.also { logger.info("Contest start time overridden to ${it.humanReadable}") }
            ?.let { it to getStateBasedOnStartTime(it, info.contestLength) }
            ?: (info.startTime to info.status)
        val freezeTime = overrides.freezeTime ?: info.freezeTime
        val holdTimeSeconds = overrides.holdTime ?: info.holdBeforeStartTime
        val medals = overrides.scoreboardOverrides?.medals ?: info.medals
        val penaltyPerWrongAttempt = overrides.scoreboardOverrides?.penaltyPerWrongAttempt ?: info.penaltyPerWrongAttempt
        val penaltyRoundingMode = overrides.scoreboardOverrides?.penaltyRoundingMode ?: info.penaltyRoundingMode
        if (unusedTeamOverrides.isNotEmpty()) logger.warn("No team for override: $unusedTeamOverrides")
        if (unusedProblemOverrides.isNotEmpty()) logger.warn("No problem for override: $unusedProblemOverrides")

        val teamInfosFiltered = if (overrides.scoreboardOverrides?.showTeamsWithoutSubmissions != false) {
            teamInfos
        } else {
            teamInfos.filter { it.id in submittedTeams }.also {
                logger.info("Filtered out ${teamInfos.size - it.size} of ${teamInfos.size} teams as they don't have submissions")
            }
        }

        logger.info("Team and problem overrides are reloaded")
        return info.copy(
            startTime = startTime,
            freezeTime = freezeTime,
            status = status,
            holdBeforeStartTime = holdTimeSeconds,
            teams = teamInfosFiltered,
            problems = problemInfos.sortedBy { it.ordinal },
            medals = medals,
            penaltyPerWrongAttempt = penaltyPerWrongAttempt,
            penaltyRoundingMode = penaltyRoundingMode,
        )
    }

    companion object {
        val logger = getLogger(AdvancedPropertiesAdapter::class)
    }
}
