package org.icpclive.cds.tunning

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.icpclive.cds.api.AwardChain
import org.icpclive.cds.api.ContestInfo
import org.icpclive.cds.api.GroupId
import org.icpclive.cds.api.RankBasedAward
import org.icpclive.cds.util.ListOrSingleElementSerializer
import org.icpclive.cds.util.getLogger
import kotlin.collections.plus

@Serializable
@SerialName("addAwardChain")
public data class AddAwardChain(
    @Serializable(with = ListOrSingleElementSerializer::class) val awards: List<RankBasedAward>,
    val groups: List<GroupId> = emptyList(),
    val excludedGroups: List<GroupId> = emptyList(),
    val organizationLimit: Int? = null,
    val organizationLimitCustomField: String? = null,
): TuningRule {

    override fun process(info: ContestInfo): ContestInfo {
        val existingIds = info.awardsSettings.flatMap { it.awards }.map { it.id }.toMutableSet()
        val badIds = awards.map { it.id }.filterNot { existingIds.add(it) }
        if (badIds.isNotEmpty()) {
            logger.error { "Can't add chain of awards with duplicate ids: $badIds" }
            return info
        }
        return info.copy(
            awardsSettings = info.awardsSettings + AwardChain(awards, groups, excludedGroups, organizationLimit, organizationLimitCustomField)
        )
    }
    public companion object {
        private val logger by getLogger()
    }
}