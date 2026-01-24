package org.icpclive.cds.api

import kotlinx.serialization.Serializable

public enum class AwardTiebreakMode { NONE, ALL; }

/**
 * Settings for rank and score-based awards.
 * Typically, medals, diplomas and honorable mentions or qualifications to the next stage.
 *
 * @param id ID of award
 * @param citation Award text.
 * @param maxRank If not null, only teams with rank of at most [maxRank] are eligible.
 * @param minScore Only teams with score of at least [minScore] are eligible. By default, any non-zero score is required to receive a medal.
 * @param tiebreakMode In case of tied ranks, if [AwardTiebreakMode.NONE] none of the teams will be awarded, if [AwardTiebreakMode.ALL] - all.
 * @param manualTeamIds Extra teams getting award no matter their rank (for example, for wildcards).
 * @param limit Limit of medals awarded to teams in the same group.
 * @param chainLimit Limit of medals awarded to teams in the same group, including those from the previous awards in the same chain.
 * @param organizationLimit Limit of medals awarded to teams in any organization.
 * @param organizationLimitCustomField Custom field in organization to be used as override of common limit.
 * @param chainOrganizationLimit Limit of medals awarded to teams in any organization, including those from the previous awards in the same chain.
 * @param chainOrganizationLimitCustomField Custom field in organization to be used as override of common limit.
 */
@Serializable
public data class RankBasedAward(
    val id: String,
    val citation: String,
    val maxRank: Int? = null,
    val minScore: Double? = null,
    val tiebreakMode: AwardTiebreakMode = AwardTiebreakMode.ALL,
    val limit: Int? = null,
    val chainLimit: Int? = null,
    val organizationLimit: Int? = null,
    val organizationLimitCustomField: String? = null,
    val chainOrganizationLimit: Int? = null,
    val chainOrganizationLimitCustomField: String? = null,
    val manualTeamIds: Set<TeamId> = emptySet(),
)

/**
 * @param awards List of possible awards, only the first matched to be awarded.
 * @param groups If not empty, only award to teams in that group
 * @param excludedGroups Do not award to a team in that group
 * @param organizationLimit Limit of medals awarded to teams in any organization.
 * @param organizationLimitCustomField Custom field in organization to be used as override of common limit.
 */
@Serializable
public data class AwardChain(
    val awards: List<RankBasedAward>,
    val groups: List<GroupId> = emptyList(),
    val excludedGroups: List<GroupId> = emptyList(),
    val organizationLimit: Int? = null,
    val organizationLimitCustomField: String? = null,
)


/**
 * Legacy settings for awards granted manually.
 *
 * @param id ID of award
 * @param citation Award text
 * @param teamCdsIds List of team cds ids to grant award.
 */
@Serializable
public data class ManualAwardSetting(
    val id: String,
    val citation: String,
    val teamCdsIds: List<TeamId>,
)