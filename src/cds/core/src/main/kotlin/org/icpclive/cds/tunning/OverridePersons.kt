package org.icpclive.cds.tunning

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.icpclive.cds.api.*
import org.icpclive.cds.util.ListOrSingleElementSerializer
import org.icpclive.cds.util.logger

@Serializable
@SerialName("overridePersons")
public data class OverridePersons(public val rules: Map<PersonId, Override>): TuningRule {
    @OptIn(InefficientContestInfoApi::class)
    override fun process(info: ContestInfo): ContestInfo {
        return info.copy(
            personsList = mergeOverrides(
                info.personsList,
                rules,
                { id },
                logUnused = { logger(OverridePersons::class).warning { "No person for override: $it" } }
            ) { person, override ->
                person.copy(
                    id = person.id,
                    name = override.name ?: person.name,
                    role = override.role ?: person.role,
                    icpcId = override.icpcId ?: person.icpcId,
                    teamIds = (override.teamIds ?: person.teamIds) + override.extraTeamIds.orEmpty(),
                    title = override.title ?: person.title,
                    email = override.email ?: person.email,
                    sex = override.sex ?: person.sex,
                    photo = (override.photo ?: person.photo) + override.extraPhoto.orEmpty(),
                )
            }
        )
    }

    @Serializable
    public class Override(
        public val name: String? = null,
        public val role: String? = null,
        public val icpcId: String? = null,
        public val teamIds: List<TeamId>? = null,
        public val extraTeamIds: List<TeamId>? = null,
        public val title: String? = null,
        public val email: String? = null,
        public val sex: String? = null,
        @Serializable(with = ListOrSingleElementSerializer::class) public val photo: List<MediaType>? = null,
        @Serializable(with = ListOrSingleElementSerializer::class) public val extraPhoto: List<MediaType>? = null,
    )
}