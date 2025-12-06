package org.icpclive.cds.tunning

import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.elementDescriptors
import kotlinx.serialization.descriptors.elementNames
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.decodeFromStream
import kotlinx.serialization.serializer
import org.icpclive.cds.api.*
import org.icpclive.cds.api.AwardsSettings.MedalGroup
import java.io.InputStream

private fun SerialDescriptor.unwrapInlines(): SerialDescriptor = if (isInline) elementDescriptors.first().unwrapInlines() else this

private inline fun <K, reified T> fieldsToOverrideImpl(rules: Map<K, Map<String, String>>) : Map<K, T> {
    val s = serializer<T>()
    val knownFields = buildSet {
        val descriptor = s.descriptor
        for ((index, element) in descriptor.elementNames.withIndex()) {
            if (descriptor.getElementDescriptor(index).unwrapInlines().kind == PrimitiveKind.STRING) {
                add(element)
            }
        }
    }
    return rules.mapValues { (_, data) ->
        val knownData = data.filterKeys { it in knownFields }
        val customData = data.filterKeys { it !in knownFields }
        Json.decodeFromJsonElement(
            s,
            buildJsonObject {
                for ((k, v) in knownData) {
                    put(k, JsonPrimitive(v))
                }
                put("customFields", JsonObject(customData.mapValues { JsonPrimitive(it.value) }))
            }
        )
    }
}


/**
 * This is a base interface for all rules in advanced.json file.
 *
 * Ideally, all information should be received from the contest system.
 * Unfortunately, in the real world, it's not always possible, or information
 * can be not fully correct or convenient to display.
 *
 * To deal with it, you can specify advanced.json file containing a list of [TuningRule]s,
 * which would update existing information loaded from the contest system.
 */
@Serializable
public sealed interface TuningRule {
    public companion object {
        private val json = Json {
            allowComments = true
            allowTrailingComma = true
        }

        public fun listFromString(input: String): List<TuningRule> = json.decodeFromString(input)
        public fun listFromInputStream(input: InputStream): List<TuningRule> = json.decodeFromStream(input)
        public fun tryListFromLegacyFormatFromString(input: String): List<TuningRule>? = runCatching {
            AdvancedProperties.fromString(input).toRulesList()
        }.getOrNull()

        public fun tryListFromLegacyFormatFromInputStream(input: InputStream): List<TuningRule>? = runCatching {
            AdvancedProperties.fromInputStream(input).toRulesList()
        }.getOrNull()

        public fun fromTeamFields(input: Map<String, Map<String, String>>): TuningRule {
            return OverrideTeams(rules = fieldsToOverrideImpl(input.mapKeys { it.key.toTeamId() }))
        }
        public fun fromOrganizationFields(input: Map<String, Map<String, String>>): TuningRule {
            return OverrideOrganizations(rules = fieldsToOverrideImpl(input.mapKeys { it.key.toOrganizationId() }))
        }
    }

    public fun process(info: ContestInfo): ContestInfo
}

// for schema generation
@Suppress("unused")
@JvmInline
@Serializable
internal value class TuningRuleList(val list: List<TuningRule>)

/**
 * A helper interface for rules that can be represented as more basic but verbose rule, if [ContestInfo] provided.
 */
public interface Desugarable {
    public fun desugar(info: ContestInfo): TuningRule
}

/**
 * A helper interface for rules that can be represented as more basic but verbose rule.
 * Mostly, such rules are just shortcuts to something more basic
 */
public interface SimpleDesugarable : Desugarable {
    public fun desugar(): TuningRule
    override fun desugar(info: ContestInfo): TuningRule = desugar()
}

private fun <K, V> mapOfNotNull(vararg pairs: Pair<K, V?>): Map<K, V> =
    pairs.filter { it.second != null }.associate { it.first to it.second!! }

internal fun AdvancedProperties.toRulesList(): List<TuningRule> = buildList buildRulesList@{
    if (contestName != null || startTime != null || contestLength != null || freezeTime != null || holdTime != null) {
        add(OverrideContestSettings(
            name = contestName,
            startTime = startTime,
            contestLength = contestLength,
            freezeTime = freezeTime,
            holdTime = holdTime
        ))
    }
    if (scoreboardOverrides != null && (scoreboardOverrides.penaltyPerWrongAttempt != null || scoreboardOverrides.showTeamsWithoutSubmissions != null || scoreboardOverrides.penaltyRoundingMode != null)) {
        add(OverrideScoreboardSettings(scoreboardOverrides.penaltyPerWrongAttempt, scoreboardOverrides.showTeamsWithoutSubmissions, scoreboardOverrides.penaltyRoundingMode))
    }
    fun TeamRegexOverrides.process(from: String) {
        if (organizationRegex != null || groupRegex != null || customFields != null) {
            add(
                OverrideTeamTemplate(
                    regexes = mapOfNotNull(
                        "org" to organizationRegex?.regexes?.let {
                            TemplateRegexParser(
                                from = from,
                                rules = it.mapValues { mapOf("id" to it.value) }
                            )
                        },
                        "group" to groupRegex?.entries?.let {
                            TemplateRegexParser(
                                from = from,
                                rules = it.associate { it.value to mapOf("id" to it.key) }
                            )
                        },
                        "custom" to customFields?.let {
                            val data = buildList {
                                for ((k, v) in it) {
                                    for ((regex, replace) in v.regexes) {
                                        add(Triple("${k}Value", regex, replace))
                                    }
                                }
                            }.groupBy({ it.second.toString() }, { it.first to it.third })
                                .mapValues { it.value.toMap() }
                            TemplateRegexParser(
                                from = from,
                                rules = data.mapKeys { Regex(it.key) }
                            )
                        }
                    ),
                    organizationId = "{regexes.org.id}".takeIf { organizationRegex != null },
                    extraGroups = listOf("{regexes.group.id}").takeIf { groupRegex != null },
                    customFields = customFields?.entries?.associate { it.key to "{regexes.custom.${it.key}Value}" }
                )
            )
        }
    }
    teamNameRegexes?.process("{team.fullName}")
    teamIdRegexes?.process("{team.id}")
    if (groupOverrides != null && groupOverrides.any { it.value.isHidden != null || it.value.isOutOfContest != null || it.value.displayName != null }) {
        add(OverrideGroups(groupOverrides))
    }
    if (organizationOverrides != null && organizationOverrides.any { it.value.fullName != null || it.value.logo != null || it.value.displayName != null }) {
        add(OverrideOrganizations(organizationOverrides))
    }

    if (teamOverrideTemplate != null) {
        val template = OverrideTeamTemplate(
            displayName = teamOverrideTemplate.displayName,
            fullName = teamOverrideTemplate.fullName,
            hashTag = teamOverrideTemplate.hashTag,
            medias = teamOverrideTemplate.medias?.mapValues { listOfNotNull(it.value) },
            color = teamOverrideTemplate.color
        )
        val serialized = Json.encodeToString(template)
        val templateFixed = Json.decodeFromString<OverrideTeamTemplate>(
            serialized
                .replace("{teamId}", "{team.id}")
                .replace("{orgDisplayName}", "{org.displayName}")
                .replace("{orgFullName}", "{org.fullName}")
        )
        add(templateFixed)
    }

    if (teamOverrides != null) {
        add(OverrideTeams(teamOverrides))
    }

    if (problemOverrides != null) {
        add(OverrideProblems(problemOverrides))
    }
    if (awardsSettings != null) {
        val (championTitle, groupChampionTitles, rankAwardsMaxRank, medalsExtra, medalGroups, manual) = awardsSettings
        fun MedalGroup.tryToMedals() : TuningRule? {
            val goldCut = medals.getOrNull(0)?.maxRank ?: return null
            val silverCut = medals.getOrNull(1)?.maxRank ?: goldCut
            val bronzeCut = medals.getOrNull(2)?.maxRank ?: silverCut
            val cand = AddMedals(goldCut, silverCut - goldCut, bronzeCut - silverCut)
            val des = cand.desugar() as? OverrideAwards ?: return null
            val group = des.extraMedalGroups?.singleOrNull() ?: return null
            return cand.takeIf { group == this }
        }
        val medals = buildList {
            val original = medalGroups + medalsExtra.takeIf { it.isNotEmpty() }?.let { MedalGroup(medalsExtra, emptyList(), emptyList()) }
            for (cand in original.filterNotNull()) {
                val simple: TuningRule? = cand.tryToMedals()
                if (simple != null) {
                    this@buildRulesList.add(simple)
                } else {
                    add(cand)
                }
            }
        }
        if (championTitle != null || groupChampionTitles.isNotEmpty() || rankAwardsMaxRank != 0 || medals.isNotEmpty() || manual.isNotEmpty()) {
            add(OverrideAwards(
                championTitle = championTitle,
                groupsChampionTitles = groupChampionTitles.takeIf { it.isNotEmpty() },
                rankAwardsMaxRank = rankAwardsMaxRank.takeIf { it != 0 },
                medalGroups = medals.takeIf { it.isNotEmpty() },
                manualAwards = manual.takeIf { it.isNotEmpty() },
            ))
        }
    }
    if (queueSettings != null) {
        add(OverrideQueue(queueSettings.waitTime, queueSettings.firstToSolveWaitTime, queueSettings.featuredRunWaitTime, queueSettings.inProgressRunWaitTime, queueSettings.maxQueueSize, queueSettings.maxUntestedRun))
    }
}

/**
 * Converts values in [ContestInfo] to overrides in [TuningRule]s.
 */
@OptIn(InefficientContestInfoApi::class)
public fun ContestInfo.toRulesList(): List<TuningRule> {
    return listOf(
        OverrideContestSettings(
            name = name,
            startTime = startTime,
            contestLength = contestLength,
            freezeTime = freezeTime,
            holdTime = (status as? ContestStatus.BEFORE)?.holdTime,
            customFields = customFields,
        ),
        OverrideTeams(
            rules = teamList.associate {
                it.id to OverrideTeams.Override(
                    fullName = it.fullName,
                    displayName = it.displayName,
                    organizationId = it.organizationId,
                    hashTag = it.hashTag,
                    groups = it.groups,
                    medias = it.medias,
                    customFields = it.customFields,
                    isHidden = it.isHidden,
                    isOutOfContest = it.isOutOfContest,
                    color = it.color,
                    extraGroups = null,
                )
            }
        ),
        OverrideProblems(
            rules = problemList.associate {
                it.id to OverrideProblems.Override(
                    displayName = it.displayName,
                    fullName = it.fullName,
                    color = it.color,
                    ordinal = it.ordinal,
                    minScore = it.minScore,
                    maxScore = it.maxScore,
                    scoreMergeMode = it.scoreMergeMode,
                    isHidden = it.isHidden,
                    weight = it.weight,
                    ftsMode = it.ftsMode,
                )
            }
        ),
        OverrideGroups(
            rules = groupList.associate {
                it.id to OverrideGroups.Override(
                    displayName = it.displayName,
                    isHidden = it.isHidden,
                    isOutOfContest = it.isOutOfContest,
                )
            }
        ),
        OverrideOrganizations(
            rules = organizationList.associate {
                it.id to OverrideOrganizations.Override(
                    displayName = it.displayName,
                    fullName = it.fullName,
                    logo = it.logo,
                    country = it.country,
                    countryFlag = it.countryFlag,
                    countrySubdivision = it.countrySubdivision,
                    countrySubdivisionFlag = it.countrySubdivisionFlag,
                    customFields = it.customFields
                )
            }
        ),
        OverridePersons(
            rules = personsList.associate {
                it.id to OverridePersons.Override(
                    name = it.name,
                    role = it.role,
                    icpcId = it.icpcId,
                    teamIds = it.teamIds,
                    title = it.title,
                    email = it.email,
                    sex = it.sex,
                    photo = it.photo,
                )
            }
        ),
        OverrideScoreboardSettings(
            penaltyPerWrongAttempt = penaltyPerWrongAttempt,
            penaltyRoundingMode = penaltyRoundingMode,
            problemColorPolicy = problemColorPolicy,
            showTeamsWithoutSubmissions = showTeamsWithoutSubmissions,
        ),
        OverrideAwards(
            championTitle = awardsSettings.championTitle,
            groupsChampionTitles = awardsSettings.groupsChampionTitles,
            rankAwardsMaxRank = awardsSettings.rankAwardsMaxRank,
            medalGroups = awardsSettings.medalGroups,
            manualAwards = awardsSettings.manual,
        ),
        OverrideQueue(
            waitTime = queueSettings.waitTime,
            firstToSolveWaitTime = queueSettings.firstToSolveWaitTime,
            featuredRunWaitTime = queueSettings.featuredRunWaitTime,
            inProgressRunWaitTime = queueSettings.inProgressRunWaitTime,
            maxQueueSize = queueSettings.maxQueueSize,
            maxUntestedRun = queueSettings.maxUntestedRun,
        ),
    )
}
