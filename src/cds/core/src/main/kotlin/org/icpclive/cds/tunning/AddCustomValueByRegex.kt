package org.icpclive.cds.tunning

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.icpclive.cds.api.ContestInfo
import org.icpclive.cds.api.InefficientContestInfoApi

@Serializable
@SerialName("add_custom_value_by_regex")
public data class AddCustomValueByRegex(
    public val name: String,
    public val from: String,
    public val rules: RegexSet
): DesugarableTuningRule {
    @OptIn(InefficientContestInfoApi::class)
    override fun desugar(info: ContestInfo): TuningRule {
        return AddCustomValue(
            name,
            info.teamList.mapNotNull {
                val fromValue = info.getTemplateValue(from, it.id, isUrl = false)
                val value = rules.applyTo(fromValue) ?: return@mapNotNull null
                it.id to value
            }.toMap()
        )
    }
}