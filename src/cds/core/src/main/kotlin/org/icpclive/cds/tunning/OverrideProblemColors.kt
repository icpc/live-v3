package org.icpclive.cds.tunning

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.icpclive.cds.api.*

@Serializable
@SerialName("override_problem_colors")
public data class OverrideProblemColors(
    val rules: Map<ProblemId, Color>
): DesugarableTuningRule {
    override fun desugar(info: ContestInfo): TuningRule {
        return OverrideProblems(rules.mapValues { ProblemInfoOverride(color = it.value) })
    }
}