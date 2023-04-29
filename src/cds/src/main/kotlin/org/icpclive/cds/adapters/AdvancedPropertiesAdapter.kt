package org.icpclive.cds.adapters

import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import org.icpclive.api.*
import org.icpclive.api.tunning.*
import org.icpclive.cds.ContestUpdate
import org.icpclive.cds.InfoUpdate
import org.icpclive.cds.RunUpdate
import org.icpclive.util.*
import kotlin.time.Duration

private sealed interface AdvancedAdapterEvent
private class Update(val update: ContestUpdate) : AdvancedAdapterEvent
private class Advanced(val update: AdvancedProperties) : AdvancedAdapterEvent
private object TimeTrigger : AdvancedAdapterEvent


object AdvancedPropertiesAdapter

fun Flow<ContestUpdate>.applyAdvancedProperties(advancedPropsFlow: Flow<AdvancedProperties>) = flow {
    val triggerFlow = Channel<TimeTrigger>()
    var contestInfo: ContestInfo? = null
    var advancedProperties: AdvancedProperties? = null
    val submittedTeams = mutableSetOf<Int>()
    val triggers = mutableSetOf<Instant>()
    coroutineScope {
        suspend fun triggerAt(time: Instant) {
            if (time < Clock.System.now()) return
            if (triggers.add(time)) {
                launch {
                    delay(time - Clock.System.now())
                    triggerFlow.send(TimeTrigger)
                }
            }
        }
        suspend fun apply() {
            val ci = contestInfo ?: return
            val ap = advancedProperties ?: return
            emit(InfoUpdate(applyOverrides(ci, ap, submittedTeams)))
            val startOverride = ap.startTime ?: return
            triggerAt(startOverride)
            triggerAt(startOverride + ci.contestLength)
        }
        merge(
            this@applyAdvancedProperties.map { Update(it) },
            triggerFlow.receiveAsFlow(),
            advancedPropsFlow.map { Advanced(it) },
        ).collect {
            when (it) {
                is TimeTrigger -> {
                    apply()
                }
                is Advanced -> {
                    advancedProperties = it.update
                    apply()
                }
                is Update -> {
                    when (it.update) {
                        is InfoUpdate -> {
                            contestInfo = it.update.newInfo
                            apply()
                        }
                        is RunUpdate -> {
                            emit(it.update)
                            if (submittedTeams.add(it.update.newInfo.teamId)) {
                                apply()
                            }
                        }

                        else -> {
                            emit(it.update)
                        }
                    }
                }
            }
        }
    }
}

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


private fun applyOverrides(
    info: ContestInfo,
    overrides: AdvancedProperties,
    submittedTeams: Set<Int>
): ContestInfo {
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
            id = team.id,
            name = override.name ?: team.name,
            shortName = override.shortname ?: team.shortName,
            contestSystemId = team.contestSystemId,
            groups = override.groups ?: team.groups,
            hashTag = override.hashTag ?: team.hashTag,
            medias = if (override.medias != null)
                (team.medias + override.medias).filterValues { it != null }.mapValues { it.value!! }
            else
                team.medias,
            additionalInfo = override.additionalInfo,
            isHidden = override.isHidden ?: team.isHidden,
            isOutOfContest = override.isOutOfContest ?: team.isOutOfContest
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
            problem.contestSystemId,
            override.minScore ?: problem.minScore,
            override.maxScore ?: problem.maxScore,
            override.color ?: problem.color,
            override.scoreMergeMode ?: problem.scoreMergeMode
        )
    }
    val (startTime, status) = overrides.startTime
        ?.also { logger.info("Contest start time overridden to ${it.humanReadable}") }
        ?.let { it to ContestStatus.byCurrentTime(it, info.contestLength) }
        ?: (info.startTime to info.status)
    val freezeTime = overrides.freezeTime ?: info.freezeTime
    val holdTimeSeconds = overrides.holdTime ?: info.holdBeforeStartTime
    val medals = overrides.scoreboardOverrides?.medals ?: info.medals
    val penaltyPerWrongAttempt =
        overrides.scoreboardOverrides?.penaltyPerWrongAttempt ?: info.penaltyPerWrongAttempt
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
    val newGroups = teamInfosFiltered.flatMap { it.groups }.toSet() - info.groups.map { it.name }.toSet()
    val (groups, unusedGroupsOverrides) = mergeOverride(
        info.groups + newGroups.map { GroupInfo(it, false, false) },
        overrides.groupOverrides,
        GroupInfo::name
    ) { group, override ->
        GroupInfo(
            name = group.name,
            isHidden = override.isHidden ?: group.isHidden,
            isOutOfContest = override.isOutOfContest ?: group.isOutOfContest
        )
    }
    if (unusedGroupsOverrides.isNotEmpty()) logger.warn("No group for override: $unusedGroupsOverrides")

    logger.info("Team and problem overrides are reloaded")
    return info.copy(
        startTime = startTime,
        freezeTime = freezeTime,
        status = status,
        holdBeforeStartTime = holdTimeSeconds,
        teams = teamInfosFiltered,
        groups = groups,
        problems = problemInfos.sortedBy { it.ordinal },
        medals = medals,
        penaltyPerWrongAttempt = penaltyPerWrongAttempt,
        penaltyRoundingMode = penaltyRoundingMode,
    )
}

private val logger = getLogger(AdvancedPropertiesAdapter::class)