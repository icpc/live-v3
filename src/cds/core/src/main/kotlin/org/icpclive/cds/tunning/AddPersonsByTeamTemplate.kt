package org.icpclive.cds.tunning

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.icpclive.cds.api.*

@Serializable
@SerialName("addPersonsByTeamTemplate")
public data class AddPersonsByTeamTemplate(
    public val regexes: Map<String, TemplateRegexParser> = emptyMap(),
    public val persons: List<PersonInfo>,
): Desugarable, TuningRule {
    override fun process(info: ContestInfo): ContestInfo {
        return desugar(info).process(info)
    }

    @OptIn(InefficientContestInfoApi::class)
    override fun desugar(info: ContestInfo): TuningRule {
        return AddPersons(
            info.teamList.flatMap { teamInfo ->
                val org = info.organizations[teamInfo.organizationId]
                with(getSubstitutor(regexes, teamInfo, org)) {
                    persons.map {
                        PersonInfo(
                            id = substituteRaw(it.id.value).toPersonId(),
                            name = substituteRaw(it.name),
                            role = substituteRaw(it.role),
                            icpcId = substituteRaw(it.icpcId),
                            teamIds = it.teamIds.map { teamId -> substituteRaw(teamId.value).toTeamId() },
                            title = substituteRaw(it.title),
                            email = substituteRaw(it.email),
                            sex = substituteRaw(it.sex),
                            photo = it.photo.map { substitute(it) },
                        )
                    }
                }
            }
        )
    }
}
