package org.icpclive.cds.plugins.cms.model

import kotlinx.serialization.Serializable
import org.icpclive.cds.util.serializers.UnixSecondsSerializer
import kotlin.time.Instant

@Serializable
internal class Submission(
    val user: String,
    val task: String,
    @Serializable(with = UnixSecondsSerializer::class)
    val time: Instant,
)