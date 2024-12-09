package org.icpclive.cds.tunning

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.icpclive.cds.api.*
import org.icpclive.cds.api.AwardsSettings.*

@Serializable
@SerialName("overrideAwards")
public data class OverrideAwards(
    public val championTitle: String? = null,
    public val groupsChampionTitles: Map<GroupId, String?>? = null,
    public val rankAwardsMaxRank: Int? = null,
    public val medalGroups: List<MedalGroup>? = null,
    public val extraMedalGroups: List<MedalGroup>? = null,
    public val manualAwards: List<ManualAwardSetting>? = null,
    public val extraManualAwards: List<ManualAwardSetting>? = null,
): TuningRule {
    override fun process(info: ContestInfo, submittedTeams: Set<TeamId>): ContestInfo {
        val old = info.awardsSettings
        return info.copy(
            awardsSettings = old.copy(
                championTitle = championTitle ?: old.championTitle,
                groupsChampionTitles = mergeMaps(old.groupsChampionTitles, groupsChampionTitles),
                rankAwardsMaxRank = rankAwardsMaxRank ?: old.rankAwardsMaxRank,
                medalGroups = (medalGroups ?: old.medalGroups) + extraMedalGroups.orEmpty(),
                manual = (manualAwards ?: old.manual) + extraManualAwards.orEmpty(),
            )
        )
    }
}

@Serializable
@SerialName("addMedals")
public data class AddMedals(
    val gold: Int = 0,
    val silver: Int = 0,
    val bronze: Int = 0,
    val tiebreakMode: MedalTiebreakMode = MedalTiebreakMode.ALL,
    val minScore: Double = Double.MIN_VALUE
) : SimpleDesugarableTuningRule {
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
}