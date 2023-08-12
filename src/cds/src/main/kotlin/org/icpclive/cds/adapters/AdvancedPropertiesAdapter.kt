package org.icpclive.cds.adapters

import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import org.icpclive.api.*
import org.icpclive.api.tunning.*
import org.icpclive.cds.*
import org.icpclive.util.getLogger
import org.icpclive.util.humanReadable

private sealed interface AdvancedAdapterEvent
private class Update(val update: ContestUpdate) : AdvancedAdapterEvent
private class Advanced(val update: AdvancedProperties) : AdvancedAdapterEvent
private object TimeTrigger : AdvancedAdapterEvent


object AdvancedPropertiesAdapter

private fun MediaType.applyTemplate(teamId: String) = when (this) {
    is MediaType.Photo -> copy(url = url.replace("{teamId}", teamId))
    is MediaType.Video -> copy(url = url.replace("{teamId}", teamId))
    is MediaType.Object -> copy(url = url.replace("{teamId}", teamId))
    is MediaType.WebRTCProxyConnection -> copy(url = url.replace("{teamId}", teamId))
    is MediaType.WebRTCGrabberConnection -> copy(
        url = url.replace("{teamId}", teamId),
        peerName = peerName.replace("{teamId}", teamId),
        credential = credential?.replace("{teamId}", teamId)
    )

    else -> this
}


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
                            if (submittedTeams.add(it.update.newInfo.teamId) && advancedProperties?.scoreboardOverrides?.showTeamsWithoutSubmissions == false) {
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

private fun <T, O> mergeOverrides(
    infos: List<T>,
    overrides: Map<String, O>?,
    id: T.() -> String,
    unusedMessage: (Set<String>) -> String? = { null },
    merge: (T, O) -> T
): List<T> {
    return if (overrides == null) {
        infos
    } else {
        val idsSet = infos.map { it.id() }.toSet()
        fun mergeIfNotNull(a: T, b: O?) = if (b == null) a else merge(a, b)
        (overrides.keys - idsSet)
            .takeIf { it.isNotEmpty() }
            ?.let(unusedMessage)
            ?.let { logger.warn(it) }
        infos.map {
            mergeIfNotNull(it, overrides[it.id()])
        }
    }
}

private fun <K, V> mergeMaps(original: Map<K, V>, override: Map<K, V?>) = buildMap {
    putAll(original)
    for ((k, v) in override.entries) {
        if (v == null) {
            remove(k)
        } else {
            put(k, v)
        }
    }
}

fun Map<TeamMediaType, MediaType?>.instantiateTemplate(teams: List<TeamInfo>) = teams.associate {
    it.contestSystemId to TeamInfoOverride(
        medias = mapValues { (_,v) -> v?.applyTemplate(it.contestSystemId) }
    )
}

private fun applyOverrides(
    info: ContestInfo,
    overrides: AdvancedProperties,
    submittedTeams: Set<Int>
): ContestInfo {
    val teamInfos = mergeTeams(
        mergeTeams(info.teams, overrides.teamMediaTemplate?.instantiateTemplate(info.teams)),
        overrides.teamOverrides
    )
    val problemInfos = mergeProblems(info.problems, overrides.problemOverrides)
    val teamInfosFiltered = if (overrides.scoreboardOverrides?.showTeamsWithoutSubmissions != false) {
        teamInfos
    } else {
        teamInfos.filter { it.id in submittedTeams }.also {
            logger.info("Filtered out ${teamInfos.size - it.size} of ${teamInfos.size} teams as they don't have submissions")
        }
    }
    val newGroups = teamInfosFiltered.flatMap { it.groups }.toSet() - info.groups.map { it.name }.toSet()
    val groups = mergeGroups(
        info.groups + newGroups.map { GroupInfo(it, false, false) },
        overrides.groupOverrides
    )
    val newOrganizations =
        teamInfosFiltered.mapNotNull { it.organizationId }.toSet() - info.organizations.map { it.cdsId }.toSet()
    val organizations = mergeOrganizations(
        info.organizations + newOrganizations.map { OrganizationInfo(it, it, it) },
        overrides.organizationOverrides
    )

    val (startTime, status) = overrides.startTime
        ?.also { logger.info("Contest start time overridden to ${it.humanReadable}") }
        ?.let { it to ContestStatus.byCurrentTime(it, info.contestLength) }
        ?: (info.startTime to info.status)

    logger.info("Team and problem overrides are reloaded")
    return info.copy(
        startTime = startTime,
        freezeTime = overrides.freezeTime ?: info.freezeTime,
        status = status,
        holdBeforeStartTime = overrides.holdTime ?: info.holdBeforeStartTime,
        teams = teamInfosFiltered,
        groups = groups,
        organizations = organizations,
        problems = problemInfos.sortedBy { it.ordinal },
        medals = overrides.scoreboardOverrides?.medals ?: info.medals,
        penaltyPerWrongAttempt = overrides.scoreboardOverrides?.penaltyPerWrongAttempt ?: info.penaltyPerWrongAttempt,
        penaltyRoundingMode = overrides.scoreboardOverrides?.penaltyRoundingMode ?: info.penaltyRoundingMode,
    )
}

private fun mergeOrganizations(
    organizationInfos: List<OrganizationInfo>,
    overrides1: Map<String, OrganizationInfoOverride>?
) = mergeOverrides(
    organizationInfos,
    overrides1,
    OrganizationInfo::cdsId,
    unusedMessage = { "No organization for override: $it" }
) { org, override ->
    OrganizationInfo(
        cdsId = org.cdsId,
        displayName = override.displayName ?: org.displayName,
        fullName = override.fullName ?: org.fullName,
    )
}

private fun mergeGroups(
    groups: List<GroupInfo>,
    overrides: Map<String, GroupInfoOverride>?
) = mergeOverrides(
    groups,
    overrides,
    GroupInfo::name,
    unusedMessage = { "No group for override: $it" }
) { group, override ->
    GroupInfo(
        name = group.name,
        isHidden = override.isHidden ?: group.isHidden,
        isOutOfContest = override.isOutOfContest ?: group.isOutOfContest
    )
}

private fun mergeProblems(
    problems: List<ProblemInfo>,
    overrides: Map<String, ProblemInfoOverride>?
) = mergeOverrides(
    problems,
    overrides,
    ProblemInfo::letter,
    unusedMessage = { "No problem for override: $it" }
) { problem, override ->
    ProblemInfo(
        letter = problem.letter,
        name = override.name ?: problem.name,
        id = problem.id,
        ordinal = override.ordinal ?: problem.ordinal,
        contestSystemId = problem.contestSystemId,
        minScore = override.minScore ?: problem.minScore,
        maxScore = override.maxScore ?: problem.maxScore,
        color = override.color ?: problem.color,
        scoreMergeMode = override.scoreMergeMode ?: problem.scoreMergeMode
    )
}

private fun mergeTeams(teams: List<TeamInfo>, overrides: Map<String, TeamInfoOverride>?) = mergeOverrides(
    teams,
    overrides,
    TeamInfo::contestSystemId,
    unusedMessage = { "No team for override: $it" },
) { team, override ->
    TeamInfo(
        id = team.id,
        fullName = override.name ?: team.fullName,
        displayName = override.shortname ?: team.displayName,
        contestSystemId = team.contestSystemId,
        groups = override.groups ?: team.groups,
        hashTag = override.hashTag ?: team.hashTag,
        medias = mergeMaps(team.medias, override.medias ?: emptyMap()),
        customFields = mergeMaps(team.customFields, override.customFields ?: emptyMap()),
        isHidden = override.isHidden ?: team.isHidden,
        isOutOfContest = override.isOutOfContest ?: team.isOutOfContest,
        organizationId = override.organizationId ?: team.organizationId
    )
}

private val logger = getLogger(AdvancedPropertiesAdapter::class)