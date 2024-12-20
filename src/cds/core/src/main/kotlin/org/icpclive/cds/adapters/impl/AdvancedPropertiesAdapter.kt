package org.icpclive.cds.adapters.impl

import io.ktor.http.*
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import org.icpclive.cds.*
import org.icpclive.cds.api.*
import org.icpclive.cds.tunning.*
import org.icpclive.cds.util.*
import org.icpclive.cds.util.serializers.HumanTimeSerializer

private sealed interface AdvancedAdapterEvent {
    data class Update(val update: ContestUpdate) : AdvancedAdapterEvent
    data object Trigger : AdvancedAdapterEvent
}

private val templateRegex = kotlin.text.Regex("\\{(!?[a-z0-9A-Z_-]*)}")

private fun String.applyTemplate(valueProvider: (String) -> String?) =
    replace(templateRegex) { valueProvider(it.groups[1]!!.value) ?: it.value }

private fun urlEncoded(block: (String) -> String?) : (String) -> String? = l@{
    block(it.removePrefix("!"))?.run {
        if (it.startsWith("!"))
            this
        else
            encodeURLParameter()
    }
}

private fun MediaType.applyTemplate(valueProvider: (String) -> String?) = when (this) {
    is MediaType.Image -> copy(url = url.applyTemplate(urlEncoded(valueProvider)))
    is MediaType.Video -> copy(url = url.applyTemplate(urlEncoded(valueProvider)))
    is MediaType.M2tsVideo -> copy(url = url.applyTemplate(urlEncoded(valueProvider)))
    is MediaType.HLSVideo -> copy(url = url.applyTemplate(urlEncoded(valueProvider)))
    is MediaType.Object -> copy(url = url.applyTemplate(urlEncoded(valueProvider)))
    is MediaType.WebRTCProxyConnection -> copy(url = url.applyTemplate(urlEncoded(valueProvider)))
    is MediaType.WebRTCGrabberConnection -> copy(
        url = url.applyTemplate(urlEncoded(valueProvider)),
        peerName = peerName.applyTemplate(valueProvider),
        credential = credential?.applyTemplate(valueProvider)
    )

    else -> this
}


internal fun applyAdvancedProperties(flow: Flow<ContestUpdate>, advancedPropsFlow: Flow<AdvancedProperties>): Flow<ContestUpdate> =
    flow {
        val triggerFlow = Channel<AdvancedAdapterEvent.Trigger>()
        val submittedTeams = mutableSetOf<TeamId>()
        val triggers = mutableSetOf<Instant>()
        coroutineScope {
            val advancedPropsStateFlow = advancedPropsFlow.stateIn(this)
            var contestInfo: ContestInfo? = null
            fun triggerAt(time: Instant) {
                if (time < Clock.System.now()) return
                if (triggers.add(time)) {
                    launch {
                        delay(time - Clock.System.now())
                        triggerFlow.send(AdvancedAdapterEvent.Trigger)
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
                flow.map { AdvancedAdapterEvent.Update(it) },
                triggerFlow.receiveAsFlow().conflate(),
                advancedPropsStateFlow.map { AdvancedAdapterEvent.Trigger },
            ).collect {
                when (it) {
                    is AdvancedAdapterEvent.Trigger -> {
                        apply()
                    }

                    is AdvancedAdapterEvent.Update -> {
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

private fun <T, O, ID> mergeOverrides(
    infos: List<T>,
    overrides: Map<ID, O>?,
    id: T.() -> ID,
    unusedMessage: (Set<ID>) -> String? = { null },
    merge: (T, O) -> T,
): List<T> {
    return if (overrides == null) {
        infos
    } else {
        val idsSet = infos.map { it.id() }.toSet()
        fun mergeIfNotNull(a: T, b: O?) = if (b == null) a else merge(a, b)
        (overrides.keys - idsSet)
            .takeIf { it.isNotEmpty() }
            ?.let(unusedMessage)
            ?.let { logger.warning { it } }
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

private fun TeamOverrideTemplate.instantiateTemplate(
    teams: List<TeamInfo>,
    valueProvider: TeamInfo.(String) -> String?,
) = teams.associate { team ->
    team.id to instantiateTemplate { team.valueProvider(it) }
}

internal fun TeamOverrideTemplate.instantiateTemplate(
    valueProvider: (String) -> String?,
): TeamInfoOverride = TeamInfoOverride(
    hashTag = hashTag?.applyTemplate(valueProvider),
    fullName = fullName?.applyTemplate(valueProvider),
    displayName = displayName?.applyTemplate(valueProvider),
    medias = medias?.mapValues { (_, v) -> v?.applyTemplate(valueProvider) },
    color = color?.applyTemplate(valueProvider)?.let { Color.normalize(it) }
)


private fun List<TeamInfo>.filterNotSubmitted(show: Boolean?, submittedTeams: Set<TeamId>) = if (show != false) {
    this
} else {
    filter { it.id in submittedTeams }.also {
        logger.info { "Filtered out ${size - it.size} of $size teams as they don't have submissions" }
    }
}

private fun applyRegex(
    teams: List<TeamInfo>,
    regexOverrides: TeamRegexOverrides?,
    key: TeamInfo.() -> String,
): List<TeamInfo> {
    if (regexOverrides == null) return teams
    return teams.map { team ->
        val newOrg = regexOverrides.organizationRegex?.applyTo(team.key())?.toOrganizationId()
        val newGroups = regexOverrides.groupRegex?.entries?.filter { (_, regex) ->
            regex.matches(team.key())
        }?.map { it.key.toGroupId() }.orEmpty()
        val newCustomFields = regexOverrides.customFields?.mapValues { (_, regex) ->
            regex.applyTo(team.key())
        }?.filterValues { it != null }?.mapValues { it.value!! }.orEmpty()

        team.copy(
            organizationId = newOrg ?: team.organizationId,
            groups = team.groups + newGroups,
            customFields = newCustomFields + team.customFields
        )
    }
}

private fun AdvancedProperties.status(info: ContestInfo): ContestStatus {
    if (startTime == null && contestLength == null && freezeTime == null && holdTime == null) return info.status
    val status = ContestStatus.byCurrentTime(startTime ?: info.startTime ?: return info.status, freezeTime ?: info.freezeTime, contestLength ?: info.contestLength).let {
        if (it is ContestStatus.BEFORE && holdTime != null) {
            it.copy(holdTime = holdTime)
        } else {
            it
        }
    }
    if (status is ContestStatus.OVER && (info.status is ContestStatus.FINALIZED || info.status is ContestStatus.RUNNING && info.status.isFake)) return info.status
    if (status == info.status) return info.status
    logger.info { "Contest status is overridden to ${status}, startTime = ${HumanTimeSerializer.format(startTime ?: info.startTimeOrZero)}, contestLength = ${(contestLength ?: info.contestLength)}" }
    return status
}

@OptIn(InefficientContestInfoApi::class)
internal fun applyAdvancedProperties(
    info: ContestInfo,
    overrides: AdvancedProperties,
    submittedTeams: Set<TeamId>,
): ContestInfo {
    val teamInfosPrelim = applyRegex(
        applyRegex(
            info.teamList.filterNotSubmitted(
                overrides.scoreboardOverrides?.showTeamsWithoutSubmissions,
                submittedTeams
            ),
            overrides.teamNameRegexes,
            { fullName }
        ),
        overrides.teamIdRegexes,
        { id.value }
    )
    val newGroups = buildSet {
        for (team in teamInfosPrelim) {
            addAll(team.groups)
        }
        overrides.teamOverrides?.values?.forEach { override ->
            override.groups?.let { addAll(it) }
        }
        for (group in info.groupList) {
            remove(group.id)
        }
    }
    val groups = mergeGroups(
        info.groupList + newGroups.map { GroupInfo(it, it.value, isHidden = false, isOutOfContest = false) },
        overrides.groupOverrides
    )
    val newOrganizations = buildSet {
        for (team in teamInfosPrelim) {
            team.organizationId?.let { add(it) }
        }
        overrides.teamOverrides?.values?.forEach { override ->
            override.organizationId?.let { add(it) }
        }
        for (org in info.organizationList) {
            remove(org.id)
        }
    }
    val organizations = mergeOrganizations(
        info.organizationList + newOrganizations.map { OrganizationInfo(it, it.value, it.value, null) },
        overrides.organizationOverrides
    )

    val orgsById = organizations.associateBy { it.id }

    fun TeamInfo.templateValueGetter(name: String): String? {
        return when (name) {
            "teamId" -> id.value
            "orgFullName" -> organizationId?.let { orgsById[it]?.fullName }
            "orgDisplayName" -> organizationId?.let { orgsById[it]?.displayName }
            else -> customFields[name]
        }
    }

    val teamInfoWithCustomFields = teamInfosPrelim
        .mergeTeams(
            overrides.teamOverrides
                ?.filterValues { it.customFields != null }
                ?.mapValues { TeamInfoOverride(customFields = it.value.customFields) }
        )

    val teamInfos = teamInfoWithCustomFields
        .mergeTeams(
            overrides.teamOverrideTemplate?.instantiateTemplate(
                teamInfoWithCustomFields,
                TeamInfo::templateValueGetter
            )
        )
        .mergeTeams(overrides.teamOverrides)
    val problemInfos = mergeProblems(info.problemList, overrides.problemOverrides)

    logger.info { "Team and problem overrides are reloaded" }
    return info.copy(
        contestLength = overrides.contestLength ?: info.contestLength,
        freezeTime = overrides.freezeTime ?: info.freezeTime,
        status = overrides.status(info),
        teamList = teamInfos,
        groupList = groups,
        organizationList = organizations,
        problemList = problemInfos,
        penaltyPerWrongAttempt = overrides.scoreboardOverrides?.penaltyPerWrongAttempt ?: info.penaltyPerWrongAttempt,
        penaltyRoundingMode = overrides.scoreboardOverrides?.penaltyRoundingMode ?: info.penaltyRoundingMode,
        awardsSettings = overrides.awardsSettings ?: info.awardsSettings,
        queueSettings = QueueSettings(
            waitTime = overrides.queueSettings?.waitTime ?: info.queueSettings.waitTime,
            firstToSolveWaitTime = overrides.queueSettings?.firstToSolveWaitTime ?: info.queueSettings.firstToSolveWaitTime,
            featuredRunWaitTime = overrides.queueSettings?.featuredRunWaitTime ?: info.queueSettings.featuredRunWaitTime,
            inProgressRunWaitTime = overrides.queueSettings?.inProgressRunWaitTime ?: info.queueSettings.inProgressRunWaitTime,
            maxQueueSize = overrides.queueSettings?.maxQueueSize ?: info.queueSettings.maxQueueSize,
            maxUntestedRun = overrides.queueSettings?.maxUntestedRun ?: info.queueSettings.maxUntestedRun,
        )
    )
}

private fun mergeOrganizations(
    organizationInfos: List<OrganizationInfo>,
    overrides1: Map<OrganizationId, OrganizationInfoOverride>?,
) = mergeOverrides(
    organizationInfos,
    overrides1,
    { id },
    unusedMessage = { "No organization for override: $it" }
) { org, override ->
    OrganizationInfo(
        id = org.id,
        displayName = override.displayName ?: org.displayName,
        fullName = override.fullName ?: org.fullName,
        logo = override.logo ?: org.logo
    )
}

private fun mergeGroups(
    groups: List<GroupInfo>,
    overrides: Map<GroupId, GroupInfoOverride>?,
) = mergeOverrides(
    groups,
    overrides,
    { id },
    unusedMessage = { "No group for override: $it" }
) { group, override ->
    GroupInfo(
        id = group.id,
        displayName = override.displayName ?: group.displayName,
        isHidden = override.isHidden ?: group.isHidden,
        isOutOfContest = override.isOutOfContest ?: group.isOutOfContest,
    )
}

private fun mergeProblems(
    problems: List<ProblemInfo>,
    overrides: Map<ProblemId, ProblemInfoOverride>?,
) = mergeOverrides(
    problems,
    overrides,
    { id },
    unusedMessage = { "No problem for override: $it" }
) { problem, override ->
    ProblemInfo(
        id = problem.id,
        displayName = override.displayName ?: problem.displayName,
        fullName = override.fullName ?: problem.fullName,
        ordinal = override.ordinal ?: problem.ordinal,
        minScore = override.minScore ?: problem.minScore,
        maxScore = override.maxScore ?: problem.maxScore,
        color = override.color ?: problem.color,
        unsolvedColor = override.unsolvedColor ?: problem.unsolvedColor,
        scoreMergeMode = override.scoreMergeMode ?: problem.scoreMergeMode,
        isHidden = override.isHidden ?: problem.isHidden,
    )
}

private fun List<TeamInfo>.mergeTeams(overrides: Map<TeamId, TeamInfoOverride>?) = mergeOverrides(
    this,
    overrides,
    { id },
    unusedMessage = { "No team for override: $it" },
) { team, override ->
    TeamInfo(
        id = team.id,
        fullName = override.fullName ?: team.fullName,
        displayName = override.displayName ?: team.displayName,
        groups = override.groups ?: team.groups,
        hashTag = override.hashTag ?: team.hashTag,
        medias = mergeMaps(team.medias, override.medias ?: emptyMap()),
        customFields = mergeMaps(team.customFields, override.customFields ?: emptyMap()),
        isHidden = override.isHidden ?: team.isHidden,
        isOutOfContest = override.isOutOfContest ?: team.isOutOfContest,
        organizationId = override.organizationId ?: team.organizationId,
        color = override.color ?: team.color,
    )
}

private val logger by getLogger()
