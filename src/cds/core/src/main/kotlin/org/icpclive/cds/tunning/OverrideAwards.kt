package org.icpclive.cds.tunning

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.icpclive.cds.api.*
import org.icpclive.cds.api.AwardsSettings.*

@Serializable
@SerialName("override_awards")
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
@SerialName("add_medals")
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

@Serializable
@SerialName("add_diplomas")
public data class AddDiplomas(
    val firstMinScore: Double? = null,
    val secondMinScore: Double? = null,
    val thirdMinScore: Double? = null,
    val honorableMentionMinScore: Double? = null,
    val tiebreakMode: MedalTiebreakMode = MedalTiebreakMode.ALL
) : SimpleDesugarableTuningRule {
    override fun desugar(): TuningRule {
        return OverrideAwards(
            extraMedalGroups = listOf(
                MedalGroup(
                    buildList {
                        if (firstMinScore != null) {
                            MedalSettings("first-diploma", "First Degree Diploma", null, null, firstMinScore, tiebreakMode)
                        }
                        if (secondMinScore != null) {
                            MedalSettings("second-diploma", "Second Degree Diploma", null, null, secondMinScore, tiebreakMode)
                        }
                        if (thirdMinScore != null) {
                            MedalSettings("third-diploma", "Third Degree Diploma", null, null, thirdMinScore, tiebreakMode)
                        }
                        if (honorableMentionMinScore != null) {
                            MedalSettings("mention", "Honorable Mention", null, null, honorableMentionMinScore, tiebreakMode)
                        }
                    }
                )
            )
        )
    }
}

@Serializable
@SerialName("add_manual_award")
public data class AddManualAward(
    val id: String,
    val citation: String,
    val teamIds: List<TeamId>,
) : SimpleDesugarableTuningRule {
    override fun desugar(): TuningRule {
        return OverrideAwards(
            extraManualAwards = listOf(ManualAwardSetting(id, citation, teamIds))
        )
    }
}