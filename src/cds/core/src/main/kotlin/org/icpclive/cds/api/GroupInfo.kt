package org.icpclive.cds.api

import kotlinx.serialization.Serializable

@JvmInline
@Serializable
public value class GroupId internal constructor(public val value: String) {
    override fun toString(): String = value
}

public fun String.toGroupId(): GroupId = GroupId(this)
public fun Int.toGroupId(): GroupId = toString().toGroupId()
public fun Long.toGroupId(): GroupId = toString().toGroupId()


@Serializable
public data class GroupInfo(
    val id: GroupId,
    val displayName: String,
    val isHidden: Boolean,
    val isOutOfContest: Boolean,
)