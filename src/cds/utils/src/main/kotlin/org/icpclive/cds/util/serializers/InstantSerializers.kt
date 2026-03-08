package org.icpclive.cds.util.serializers

import kotlinx.serialization.KSerializer
import kotlinx.serialization.builtins.serializer
import org.icpclive.cds.util.map
import kotlin.time.Instant

public object UnixMillisecondsSerializer : KSerializer<Instant> by Long.serializer().map(
    "InstantMilliseconds",
    { Instant.fromEpochMilliseconds(it) },
    { it.toEpochMilliseconds() },
)

public object UnixSecondsSerializer : KSerializer<Instant> by Long.serializer().map(
    "InstantSeconds",
    { Instant.fromEpochSeconds(it) },
    { it.epochSeconds },
)