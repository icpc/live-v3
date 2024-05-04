package org.icpclive.cds.plugins.cms.model

import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable
import org.icpclive.cds.util.serializers.UnixSecondsSerializer

@Serializable
internal data class Contest(
    val name: String,
    @Serializable(with = UnixSecondsSerializer::class)
    val begin: Instant,
    @Serializable(with = UnixSecondsSerializer::class)
    val end: Instant,
)