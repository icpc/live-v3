package org.icpclive.cds.api

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@JvmInline
@Serializable
public value class PersonId internal constructor(public val value: String) {
    override fun toString(): String = value
}

public fun String.toPersonId(): PersonId = PersonId(this)
public fun Int.toPersonId(): PersonId = toString().toPersonId()
public fun Long.toPersonId(): PersonId = toString().toPersonId()


@Serializable
public data class PersonInfo(
    val id: PersonId,
    val name: String,
    val role: String,
    val icpcId: String? = null,
    val teamIds: List<TeamId> = emptyList(),
    val title: String? = null,
    val email: String? = null,
    val sex: String? = null,
    val photo: List<MediaType> = emptyList(),
)