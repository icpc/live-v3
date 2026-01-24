package org.icpclive.cds.tunning

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.icpclive.cds.api.*

/**
 * A short-cut rule to add known awards.
 *
 * Most of the awards are only useful for export and don't affect visual representation in overlay.
 *
 * @param medals settings for medals
 * @param championTitle citation for award for winner of the contest
 * @param groupsChampionTitles map from group id to citation of award for the best team in the group
 * @param rankAwardsMaxRank How many awards of "Rank #rank" to award
 */
@Serializable
@SerialName("addKnownAwards")
public data class AddKnownAwards(
    public val medals: MedalSettings? = null,
    public val championTitle: String? = null,
    public val groupsChampionTitles: Map<GroupId, String>? = null,
    public val rankAwardsMaxRank: Int? = null,
): TuningRule {

    private fun ordinalText(x: Int) = if (x in 11..13) {
        "$x-th"
    } else {
        val r = x % 10
        val suffix = when (r) {
            1 -> "st"
            2 -> "nd"
            3 -> "rd"
            else -> "th"
        }
        "$x-$suffix"
    }

    @OptIn(InefficientContestInfoApi::class)
    override fun process(info: ContestInfo): ContestInfo {
        val toAdd = buildList {
            medals?.run {
                add(
                    AddAwardChain(
                        listOfNotNull(
                            RankBasedAward("gold-medal", "Gold Medal", gold, minScore, tiebreakMode).takeIf { gold > 0 },
                            RankBasedAward("silver-medal", "Silver Medal", gold + silver, minScore, tiebreakMode).takeIf { silver > 0 },
                            RankBasedAward("bronze-medal", "Bronze Medal", gold + silver + bronze, minScore, tiebreakMode).takeIf { bronze > 0 },
                        )
                    )
                )
            }
            if (championTitle != null) {
                add(
                    AddAwardChain(
                        awards = listOf(RankBasedAward("winner", championTitle, maxRank = 1))
                    )
                )
            }
            for ((groupId, title) in groupsChampionTitles.orEmpty()) {
                add(
                    AddAwardChain(
                        awards = listOf(RankBasedAward("group-winner-${groupId.value}", title, maxRank = 1)),
                        groups = listOf(groupId),
                    )
                )
            }
            if (rankAwardsMaxRank != null) {
                add(
                    AddAwardChain(
                        awards = (1..rankAwardsMaxRank).map { rank ->
                            RankBasedAward(
                                id = "rank-$rank",
                                citation = "${ordinalText(rank)} place",
                                maxRank = rank,
                            )
                        }
                    )
                )
            }
        }
        return toAdd.fold(info) { acc, rule -> rule.process(acc) }
    }
}

@Serializable
public data class MedalSettings(
    val gold: Int = 0,
    val silver: Int = 0,
    val bronze: Int = 0,
    val minScore: Double? = null,
    val tiebreakMode: AwardTiebreakMode = AwardTiebreakMode.ALL,
)
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
    val tiebreakMode: AwardTiebreakMode = AwardTiebreakMode.ALL,
) : SimpleDesugarable, TuningRule {
    override fun desugar(): TuningRule {
        return AddKnownAwards(
            medals = MedalSettings(gold, silver, bronze, minScore, tiebreakMode)
        )
    }

    override fun process(info: ContestInfo): ContestInfo {
        return desugar().process(info)
    }
}