package org.icpclive.cds.api

import kotlinx.serialization.Serializable

@JvmInline
@Serializable
public value class GroupId(public val value: String) {
    override fun toString(): String = value
}

@Serializable
public data class GroupInfo(
    val id: GroupId,
    val displayName: String,
    val isHidden: Boolean,
    val isOutOfContest: Boolean,
)