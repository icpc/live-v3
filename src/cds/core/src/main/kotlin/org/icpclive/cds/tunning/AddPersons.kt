package org.icpclive.cds.tunning

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.icpclive.cds.api.*
import org.icpclive.cds.util.getLogger
import org.icpclive.cds.util.logger

@Serializable
@SerialName("addPersons")
public data class AddPersons(public val persons: List<PersonInfo>): TuningRule {
    @OptIn(InefficientContestInfoApi::class)
    override fun process(info: ContestInfo): ContestInfo {
        val existingIds = info.personsList.map { it.id }.toMutableSet()
        return info.copy(
            personsList = buildList {
                addAll(info.personsList)
                for (person in persons) {
                    if (existingIds.add(person.id)) {
                        add(person)
                    } else {
                        logger(AddPersons::class).warning { "Can't add person ${person.id}: it already exists" }
                    }
                }
            }
        )
    }

    private companion object {
        val logger by getLogger()
    }
}