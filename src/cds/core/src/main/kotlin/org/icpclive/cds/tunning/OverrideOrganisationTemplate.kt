package org.icpclive.cds.tunning

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.icpclive.cds.api.*

/**
 * This is a rule, allowing to update many teams in the same way.
 *
 * Each field is transformed to the corresponding field of [OverrideOrganizations.Override].
 *
 * All string values are used after substitution of templates of form `{variableName}` to corresponding values.
 * The following variable names are supported:
 * * `{org.id}`, `{org.displayName}, `{org.fullName}` - corresponding property of team's organization.
 * * `{regexes.group.value}` - values parsed by [regexes] field of current rule, see later for details
 *
 * The operation is atomic, all updates happen at the same time. So substitution can't use value set by this operation.
 * If you need this, you can add several instances of the rule.
 *
 * @see [OverrideTeamTemplate] for details on how [regexes] work.
 *
 */
@Serializable
@SerialName("overrideOrganisationTemplate")
public data class OverrideOrganisationTemplate(
    public val regexes: Map<String, TemplateRegexParser> = emptyMap(),
    public val fullName: String? = null,
    public val displayName: String? = null,
    public val logo: MediaType? = null,
): Desugarable, TuningRule {
    override fun process(info: ContestInfo): ContestInfo {
        return desugar(info).process(info)
    }

    @OptIn(InefficientContestInfoApi::class)
    override fun desugar(info: ContestInfo): TuningRule {
        return OverrideOrganizations(
            info.organizationList.associate { orgInfo ->
                with(getSubstitutor(regexes, null, orgInfo)) {
                    orgInfo.id to OverrideOrganizations.Override(
                        fullName = substituteRaw(fullName),
                        displayName = substituteRaw(displayName),
                        logo = substitute(logo)
                    )
                }
            }
        )
    }
}
