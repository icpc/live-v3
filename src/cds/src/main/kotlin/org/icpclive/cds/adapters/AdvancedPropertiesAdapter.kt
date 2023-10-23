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
private data class Update(val update: ContestUpdate) : AdvancedAdapterEvent
private data object Trigger : AdvancedAdapterEvent


internal object AdvancedPropertiesAdapter

private val templateRegex = kotlin.text.Regex("\\{([a-z0-9A-Z_-]*)}")

private fun String.applyTemplate(valueProvider: (String) -> String?) =
    replace(templateRegex) { valueProvider(it.groups[1]!!.value) ?: it.value }

private fun MediaType.applyTemplate(valueProvider: (String) -> String?) = when (this) {
    is MediaType.Photo -> copy(url = url.applyTemplate(valueProvider))
    is MediaType.Video -> copy(url = url.applyTemplate(valueProvider))
    is MediaType.Object -> copy(url = url.applyTemplate(valueProvider))
    is MediaType.WebRTCProxyConnection -> copy(url = url.applyTemplate(valueProvider))
    is MediaType.WebRTCGrabberConnection -> copy(
        url = url.applyTemplate(valueProvider),
        peerName = peerName.applyTemplate(valueProvider),
        credential = credential?.applyTemplate(valueProvider)
    )

    else -> this
}


public fun Flow<ContestUpdate>.applyAdvancedProperties(advancedPropsFlow: Flow<AdvancedProperties>): Flow<ContestUpdate> = flow {
    val triggerFlow = Channel<Trigger>()
    val submittedTeams = mutableSetOf<Int>()
    val triggers = mutableSetOf<Instant>()
    coroutineScope {
        val advancedPropsStateFlow = advancedPropsFlow.stateIn(this)
        var contestInfo: ContestInfo? = null
        suspend fun triggerAt(time: Instant) {
            if (time < Clock.System.now()) return
            if (triggers.add(time)) {
                launch {
                    delay(time - Clock.System.now())
                    triggerFlow.send(Trigger)
                }
            }
        }
        suspend fun apply() {
            val ci = contestInfo ?: return
            val ap = advancedPropsStateFlow.value
            emit(InfoUpdate(applyAdvancedProperties(ci, ap, submittedTeams)))
            val startOverride = ap.startTime ?: return
            triggerAt(startOverride)
            triggerAt(startOverride + ci.contestLength)
        }
        merge(
            this@applyAdvancedProperties.map { Update(it) },
            triggerFlow.receiveAsFlow().conflate(),
            advancedPropsStateFlow.map { Trigger },
        ).collect {
            when (it) {
                is Trigger -> {
                    apply()
                }
                is Update -> {
                    when (it.update) {
                        is InfoUpdate -> {
                            if (contestInfo != it.update.newInfo) {
                                contestInfo = it.update.newInfo
                                apply()
                            }
                        }
                        is RunUpdate -> {
                            if (submittedTeams.add(it.update.newInfo.teamId) && advancedPropsStateFlow.value.scoreboardOverrides?.showTeamsWithoutSubmissions == false) {
                                apply()
                            }
                            emit(it.update)
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

private fun Map<TeamMediaType, MediaType?>.instantiateTemplate(teams: List<TeamInfo>, valueProvider: TeamInfo.(String) -> String?) = teams.associate {
    it.contestSystemId to TeamInfoOverride(
        medias = mapValues { (_,v) -> v?.applyTemplate { name -> it.valueProvider(name) } }
    )
}

private fun TeamOverrideTemplate.instantiateTemplate(teams: List<TeamInfo>, valueProvider: TeamInfo.(String) -> String?) = teams.associate {
    it.contestSystemId to TeamInfoOverride(
        fullName = fullName?.applyTemplate { name -> it.valueProvider(name) },
        displayName = displayName?.applyTemplate { name -> it.valueProvider(name) },
        medias = medias?.mapValues { (_,v) -> v?.applyTemplate { name -> it.valueProvider(name) } }
    )
}

private fun List<TeamInfo>.filterNotSubmitted(show: Boolean?, submittedTeams: Set<Int>) = if (show != false) {
    this
} else {
    filter { it.id in submittedTeams }.also {
        logger.info("Filtered out ${size - it.size} of $size teams as they don't have submissions")
    }
}

private fun String.matchSingleGroupRegex(regex: Regex?, name: String) : String? {
    if (regex == null) return null
    val match = regex.matchAt(this, 0)
    return if (match != null) {
        if (match.groups.size != 2) {
            logger.warn("${name.replaceFirstChar { it.uppercase() }} should match single group for ${this}, but ${match.groups.size} matched")
            null
        } else {
            this.substring(match.groups[1]!!.range)
        }
    } else {
        logger.warn("$this didn't match $name")
        null
    }
}

private fun applyRegex(teams: List<TeamInfo>, regexOverrides: TeamRegexOverrides?, key: TeamInfo.() -> String) : List<TeamInfo> {
    if (regexOverrides == null) return teams
    return teams.map { team ->
        val newOrg = team.key().matchSingleGroupRegex(regexOverrides.organizationRegex, "organization regex")
        val newGroups = regexOverrides.groupRegex?.entries?.filter { (_, regex) ->
            regex.matches(team.key())
        }?.map { it.key }.orEmpty()
        val newCustomFields = regexOverrides.customFields?.mapValues { (name, regex) ->
            team.key().matchSingleGroupRegex(regex, "$name regex")
        }?.filterValues { it != null }?.mapValues { it.value!! }.orEmpty()

        team.copy(
            organizationId = newOrg ?: team.organizationId,
            groups = team.groups + newGroups,
            customFields = newCustomFields + team.customFields
        )
    }
}

private fun AdvancedProperties.status(info: ContestInfo) : ContestStatus {
    if (startTime == null && contestLength == null) return info.status
    val status = ContestStatus.byCurrentTime(startTime ?: info.startTime, contestLength ?: info.contestLength)
    if (status == ContestStatus.OVER && (info.status == ContestStatus.FINALIZED || info.status == ContestStatus.FAKE_RUNNING)) return info.status
    if (status == info.status) return info.status
    logger.info("Contest status is overridden to ${status}, startTime = ${(startTime ?: info.startTime).humanReadable}, contestLength = ${(contestLength ?: info.contestLength)}")
    return status
}

@OptIn(InefficientContestInfoApi::class)
internal fun applyAdvancedProperties(
    info: ContestInfo,
    overrides: AdvancedProperties,
    submittedTeams: Set<Int>
): ContestInfo {
    val teamInfosPrelim = applyRegex(
        applyRegex(
            info.teamList.filterNotSubmitted(overrides.scoreboardOverrides?.showTeamsWithoutSubmissions, submittedTeams),
            overrides.teamNameRegexes,
            TeamInfo::fullName
        ),
        overrides.teamIdRegexes,
        TeamInfo::contestSystemId
    )
    val newGroups = buildSet {
        for (team in teamInfosPrelim) {
            addAll(team.groups)
        }
        overrides.teamOverrides?.values?.forEach { override ->
            override.groups?.let { addAll(it) }
        }
        for (group in info.groupList) {
            remove(group.cdsId)
        }
    }
    val groups = mergeGroups(
        info.groupList + newGroups.map { GroupInfo(it, it, isHidden = false, isOutOfContest = false) },
        overrides.groupOverrides
    )
    val newOrganizations = buildSet {
        for (team in teamInfosPrelim) {
            team.organizationId?.let { add(it) }
        }
        overrides.teamOverrides?.values?.forEach { override ->
            override.organizationId?.let { add(it) }
        }
        for (group in info.organizationList) {
            remove(group.cdsId)
        }
    }
    val organizations = mergeOrganizations(
        info.organizationList + newOrganizations.map { OrganizationInfo(it, it, it, null) },
        overrides.organizationOverrides
    )

    val orgsById = organizations.associateBy { it.cdsId }

    fun TeamInfo.templateValueGetter(name: String) : String? {
        return when (name) {
            "teamId" -> contestSystemId
            "orgFullName" -> organizationId?.let { orgsById[it]?.fullName }
            "orgDisplayName" -> organizationId?.let { orgsById[it]?.displayName }
            else -> customFields[name]
        }
    }


    @Suppress("DEPRECATION") val teamInfos = teamInfosPrelim
        .mergeTeams(overrides.teamMediaTemplate?.instantiateTemplate(teamInfosPrelim, TeamInfo::templateValueGetter))
        .mergeTeams(overrides.teamOverrideTemplate?.instantiateTemplate(teamInfosPrelim, TeamInfo::templateValueGetter))
        .mergeTeams(overrides.teamOverrides)
    val problemInfos = mergeProblems(info.problemList, overrides.problemOverrides)

    logger.info("Team and problem overrides are reloaded")
    return info.copy(
        startTime = overrides.startTime ?: info.startTime,
        contestLength = overrides.contestLength ?: info.contestLength,
        freezeTime = overrides.freezeTime ?: info.freezeTime,
        status = overrides.status(info),
        holdBeforeStartTime = overrides.holdTime ?: info.holdBeforeStartTime,
        teamList = teamInfos,
        groupList = groups,
        organizationList = organizations,
        problemList = problemInfos,
        penaltyPerWrongAttempt = overrides.scoreboardOverrides?.penaltyPerWrongAttempt ?: info.penaltyPerWrongAttempt,
        penaltyRoundingMode = overrides.scoreboardOverrides?.penaltyRoundingMode ?: info.penaltyRoundingMode,
        awardsSettings = overrides.awardsSettings ?: info.awardsSettings
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
        logo = override.logo ?: org.logo
    )
}

private fun mergeGroups(
    groups: List<GroupInfo>,
    overrides: Map<String, GroupInfoOverride>?
) = mergeOverrides(
    groups,
    overrides,
    GroupInfo::cdsId,
    unusedMessage = { "No group for override: $it" }
) { group, override ->
    GroupInfo(
        cdsId = group.cdsId,
        displayName = override.displayName ?: group.displayName,
        isHidden = override.isHidden ?: group.isHidden,
        isOutOfContest = override.isOutOfContest ?: group.isOutOfContest,
    )
}

private fun mergeProblems(
    problems: List<ProblemInfo>,
    overrides: Map<String, ProblemInfoOverride>?
) = mergeOverrides(
    problems,
    overrides,
    ProblemInfo::contestSystemId,
    unusedMessage = { "No problem for override: $it" }
) { problem, override ->
    ProblemInfo(
        displayName = override.displayName ?: problem.displayName,
        fullName = override.fullName ?: problem.fullName,
        id = problem.id,
        ordinal = override.ordinal ?: problem.ordinal,
        contestSystemId = problem.contestSystemId,
        minScore = override.minScore ?: problem.minScore,
        maxScore = override.maxScore ?: problem.maxScore,
        color = override.color ?: problem.color,
        scoreMergeMode = override.scoreMergeMode ?: problem.scoreMergeMode
    )
}

private fun List<TeamInfo>.mergeTeams(overrides: Map<String, TeamInfoOverride>?) = mergeOverrides(
    this,
    overrides,
    TeamInfo::contestSystemId,
    unusedMessage = { "No team for override: $it" },
) { team, override ->
    TeamInfo(
        id = team.id,
        fullName = override.fullName ?: team.fullName,
        displayName = override.displayName ?: team.displayName,
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