package org.icpclive.cds.tunning

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.icpclive.cds.api.*
import org.icpclive.cds.api.AwardsSettings.*

/**
 * A rule specifying awards to be given to teams
 *
 * All fields can be null, existing values are not changed in that case.
 *
 * Most of the awards are only useful for export, and don't affect visual representation in overlay.
 *
 * @param firstToSolveProblems are first-to-solved enabled in contest
 * @param championTitle citation for award for winner of the contest
 * @param groupsChampionTitles map from group id to citation of award for best team in the group
 * @param rankAwardsMaxRank How many awards of "Rank #rank" to award
 * @param medalGroups List of medal types to award. Medals inside one time are mutually exclusive (like gold/silver/bronze medal), medals inside different groups are now (like medal and diploma)
 * @param extraMedalGroups Same as [medalGroups], but don't remove existing ones.
 * @param manualAwards A list of awards with specified number of teams to receive. Useful for complex awards like "qualified to next stage".
 * @param extraManualAwards Same as [manualAwards], but don't remove existing ones.
 */
@Serializable
@SerialName("overrideAwards")
public data class OverrideAwards(
    public val firstToSolveProblems: Boolean? = null,
    public val championTitle: String? = null,
    public val groupsChampionTitles: Map<GroupId, String?>? = null,
    public val rankAwardsMaxRank: Int? = null,
    public val medalGroups: List<MedalGroup>? = null,
    public val extraMedalGroups: List<MedalGroup>? = null,
    public val manualAwards: List<ManualAwardSetting>? = null,
    public val extraManualAwards: List<ManualAwardSetting>? = null,
): TuningRule {
    override fun process(info: ContestInfo): ContestInfo {
        val old = info.awardsSettings
        return info.copy(
            awardsSettings = old.copy(
                firstToSolveProblems = firstToSolveProblems ?: old.firstToSolveProblems,
                championTitle = championTitle ?: old.championTitle,
                groupsChampionTitles = mergeMaps(old.groupsChampionTitles, groupsChampionTitles),
                rankAwardsMaxRank = rankAwardsMaxRank ?: old.rankAwardsMaxRank,
                medalGroups = (medalGroups ?: old.medalGroups) + extraMedalGroups.orEmpty(),
                manual = (manualAwards ?: old.manual) + extraManualAwards.orEmpty(),
            )
        )
    }
}

/**
 * A short-cut rule to specify medals only for purposes of scoreboard in overlay
 *
 * Would use some default ids/citations for export.
 * @param gold number of gold medals
 * @param silver number of silver medals
 * @param bronze number of bronze medals
 * @param tiebreakMode rule how medals are distributed in case of tie
 * @param minScore Minimal score (number of problems in ICPC mode) to receiver medal.
 */
@Serializable
@SerialName("addMedals")
public data class AddMedals(
    val gold: Int = 0,
    val silver: Int = 0,
    val bronze: Int = 0,
    val minScore: Double? = null,
    val tiebreakMode: MedalTiebreakMode = MedalTiebreakMode.ALL,
) : SimpleDesugarable, TuningRule {
    override fun desugar(): TuningRule {
        return OverrideAwards(
            extraMedalGroups = listOf(
                MedalGroup(
                    listOfNotNull(
                        MedalSettings("gold-medal", "Gold Medal", Award.Medal.MedalColor.GOLD, gold, minScore, tiebreakMode).takeIf { gold > 0 },
                        MedalSettings("silver-medal", "Silver Medal", Award.Medal.MedalColor.SILVER, gold + silver, minScore, tiebreakMode).takeIf { silver > 0 },
                        MedalSettings("bronze-medal", "Bronze Medal", Award.Medal.MedalColor.BRONZE, gold + silver + bronze, minScore, tiebreakMode).takeIf { bronze > 0 },
                    )
                )
            )
        )
    }

    override fun process(info: ContestInfo): ContestInfo {
        return desugar().process(info)
    }
}