package org.icpclive.cds.plugins.cms.model

import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable
import org.icpclive.cds.util.serializers.UnixSecondsSerializer

@Serializable
internal class Submission(
    val user: String,
    val task: String,
    @Serializable(with = UnixSecondsSerializer::class)
    val time: Instant,
)