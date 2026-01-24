package org.icpclive.cds.tunning

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromStream
import org.icpclive.cds.api.*
import org.icpclive.cds.util.getLogger
import org.icpclive.cds.util.serializers.*
import java.io.InputStream
import kotlin.time.Duration
import kotlin.time.Instant

@Serializable
internal class RankingSettings(
    @Serializable(with = DurationInMinutesSerializer::class)
    val penaltyPerWrongAttempt: Duration? = null,
    val showTeamsWithoutSubmissions: Boolean? = null,
    val penaltyRoundingMode: PenaltyRoundingMode? = null,
)


/**
 * This class represents possible contest configuration overrides.
 *
 * Ideally, all this information should be received from the contest system.
 * Unfortunately, in the real world, it's not always possible, or information
 * can be not fully correct or convenient to display.
 *
 * This class contains the data to be fixed in what is received from a contest system.
 *
 * The order in which overrides applied:
 *   * Time and scoreboard related (they don't interact with others)
 *   * Filtering out non-submitted teams if requested
 *   * Regexp overrides by team name (so values provided by them can be used in templates)
 *   * Regexp overrides by team id (so values provided by them can be used in templates)
 *   * Creating new groups mentioned in teams or overrides and group overrides
 *   * Creating new organizations mentioned in teams or overrides and organization overrides
 *   * Custom fields from team overrides (so values provided by them can be used in templates)
 *   * (Deprecated) Team media template
 *   * Team override template
 *   * Normal team overrides
 *
 * In all templates in all strings inside `{variableName}` pattern is substituted.
 * The following variable names are supported:
 *   * teamId - team id from the contest system
 *   * orgFullName - fullName of team organization
 *   * orgDisplayName - displayName of team organization
 *   * all values from customFields
 *
 * @param startTime Override for contest start time.
 *        The preferred format is `yyyy-mm-dd hh:mm:ss`, but some others would be accepted too.
 *        startTime override also can affect contest state.
 * @param contestLength Length of the contest. Also, can affect contest state.
 * @param freezeTime Time from the start of the contest before scoreboard freezing.
 * @param holdTime Fixed time to show as time before the contest start
 * @param teamOverrideTemplate Template for team overrides
 * @param teamNameRegexes Bunch of regexes to extract information cds doesn't provide from team name.
 * @param teamIdRegexes Bunch of regexes to extract information cds doesn't provide from team's ID.
 * @param teamOverrides Overrides for a specific team. Team id from the contest system is key.
 * @param groupOverrides Overrides for specific groups. Group name is key.
 * @param problemOverrides Overrides for specific problems. Problem id from the contest system is key.
 * @param scoreboardOverrides Overrides of scoreboard calculation settings
 */
@Serializable
internal class AdvancedProperties(
    val contestName: String? = null,
    @Serializable(with = HumanTimeSerializer::class)
    val startTime: Instant? = null,
    @Serializable(with = DurationInSecondsSerializer::class)
    val contestLength: Duration? = null,
    @Serializable(with = DurationInSecondsSerializer::class)
    @SerialName("freezeTimeSeconds")
    val freezeTime: Duration? = null,
    @Serializable(with = DurationInSecondsSerializer::class)
    @SerialName("holdTimeSeconds")
    val holdTime: Duration? = null,
    val teamOverrideTemplate: TeamOverrideTemplate? = null,
    val teamNameRegexes: TeamRegexOverrides? = null,
    val teamIdRegexes: TeamRegexOverrides? = null,
    val teamOverrides: Map<TeamId, OverrideTeams.Override>? = null,
    val groupOverrides: Map<GroupId, OverrideGroups.Override>? = null,
    val organizationOverrides: Map<OrganizationId, OverrideOrganizations.Override>? = null,
    val problemOverrides: Map<ProblemId, OverrideProblems.Override>? = null,
    val scoreboardOverrides: RankingSettings? = null,
    val awardsSettings: AwardsSettingsOverride? = null,
    val queueSettings: QueueSettingsOverride? = null,
) {
    companion object {
        private val json = Json {
            allowComments = true
            allowTrailingComma = true
        }
        fun fromString(input: String): AdvancedProperties = json.decodeFromString<AdvancedProperties>(input)
        fun fromInputStream(input: InputStream): AdvancedProperties = json.decodeFromStream<AdvancedProperties>(input)
    }
}

internal typealias Regex = @Serializable(with = RegexSerializer::class) kotlin.text.Regex

@Serializable
@JvmInline
public value class RegexSet(public val regexes: Map<Regex, String>) {
    public fun applyTo(data: String): String? {
        val matched = regexes.entries.filter { data.matches(it.key) }
        return when (matched.size) {
            0 -> {
                logger.warning { "None of regexes ${regexes.map { it.key }} didn't match $this" }
                null
            }

            1 -> {
                val (regex, replace) = matched.single()
                try {
                    data.replace(regex, replace)
                } catch (e: RuntimeException) {
                    logger.warning { "Failed to apply $regex -> $replace to ${this}: ${e.message}" }
                    null
                }
            }

            else -> {
                logger.warning { "Multiple regexes ${matched.map { it.key }} match $this" }
                null
            }
        }
    }

    private companion object {
        val logger by getLogger()
    }
}

/**
 * In some cases, the contest system provides some useful information as part of the team name.
 * This can be used to extract this information to something more structured.
 *
 * All regexes are java regex.
 *
 * @property organizationRegex The only matched regex replacement would be equal to new organization id for the team.
 * @property customFields The only matched regex replacement would be set as custom field value for the corresponding key
 * @property groupRegex The group is added if the name matches regex.
 */
@Serializable
internal class TeamRegexOverrides(
    val organizationRegex: RegexSet? = null,
    val customFields: Map<String, RegexSet>? = null,
    val groupRegex: Map<String, Regex>? = null,
)

@Serializable
internal class TeamOverrideTemplate(
    val displayName: String? = null,
    val fullName: String? = null,
    val hashTag: String? = null,
    val medias: Map<TeamMediaType, MediaType?>? = null,
    val color: String? = null,
)

@Serializable
internal data class QueueSettingsOverride(
    @Serializable(with = DurationInSecondsSerializer::class)
    @SerialName("waitTimeSeconds")
    val waitTime: Duration? = null,
    @Serializable(with = DurationInSecondsSerializer::class)
    @SerialName("firstToSolveWaitTimeSeconds")
    val firstToSolveWaitTime: Duration? = null,
    @Serializable(with = DurationInSecondsSerializer::class)
    @SerialName("featuredRunWaitTimeSeconds")
    val featuredRunWaitTime: Duration? = null,
    @Serializable(with = DurationInSecondsSerializer::class)
    @SerialName("inProgressRunWaitTimeSeconds")
    val inProgressRunWaitTime: Duration? = null,
    val maxQueueSize: Int? = null,
    val maxUntestedRun: Int? = null,
)

@Serializable
internal data class AwardsSettingsOverride(
    val championTitle: String? = null,
    val groupsChampionTitles: Map<GroupId, String> = emptyMap(),
    val rankAwardsMaxRank: Int = 0,
    val medals: List<RankBasedAward> = emptyList(),
    val medalGroups: List<AwardChain> = emptyList(),
    val manual: List<ManualAwardSetting> = emptyList(),
)