package org.icpclive.cds.api

import kotlinx.serialization.Serializable

/**
 * @param firstToSolveProblems are FTS awards enabled?
 * @param championTitle If not null, the winner award with the corresponding title would be generated
 * @param groupsChampionTitles For group ids used as keys, a group champion award with corresponding value as title would be generated
 * @param rankAwardsMaxRank For first [rankAwardsMaxRank] places award stating "this place" would be awarded
 * @param medalGroups List of rank and score-based awards. Only the first applicable in each list is awarded to a team.
 * @param manual List of awards with a manual team list.
 */
@Serializable
public data class AwardsSettings(
    public val firstToSolveProblems: Boolean = true,
    public val championTitle: String? = null,
    public val groupsChampionTitles: Map<GroupId, String> = emptyMap(),
    public val rankAwardsMaxRank: Int = 0,
    public val medalGroups: List<MedalGroup> = emptyList(),
    public val manual: List<ManualAwardSetting> = emptyList(),
) {
    public enum class MedalTiebreakMode { NONE, ALL; }

    /**
     * Settings for rank and score-based awards. Typically, medals, diplomas and honorable mentions.
     * For the purpose of this API, all of them are called medals.
     *
     * @param id ID of award
     * @param citation Award text.
     * @param color Color of award to highlight teams in overlay
     * @param maxRank If not null, only teams with rank of at most [maxRank] are eligible.
     * @param minScore Only teams with score of at least [minScore] are eligible. By default, any non-zero score is required to receive a medal.
     * @param tiebreakMode In case of tied ranks, if [MedalTiebreakMode.NONE] none of the teams will be awarded, if [MedalTiebreakMode.ALL] - all.
     */
    @Serializable
    public data class MedalSettings(
        val id: String,
        val citation: String,
        val color: Award.Medal.MedalColor? = null,
        val maxRank: Int? = null,
        val minScore: Double? = null,
        val tiebreakMode: MedalTiebreakMode = MedalTiebreakMode.ALL,
    )

    /**
     * @param medals List of medals, first to be awarded.
     * @param groups If not empty, only award to teams in that groups
     * @param excludedGroups Do not award to team in that group
     */
    @Serializable
    public data class MedalGroup(
        val medals: List<MedalSettings>,
        val groups: List<GroupId> = emptyList(),
        val excludedGroups: List<GroupId> = emptyList(),
    )


    /**
     * Settings for awards granted manually.
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
}