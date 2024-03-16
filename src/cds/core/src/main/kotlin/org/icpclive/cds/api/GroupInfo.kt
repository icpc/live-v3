package org.icpclive.cds.api

import kotlinx.serialization.Serializable

@Serializable
public data class GroupInfo(
    val cdsId: String,
    val displayName: String,
    val isHidden: Boolean,
    val isOutOfContest: Boolean,
)