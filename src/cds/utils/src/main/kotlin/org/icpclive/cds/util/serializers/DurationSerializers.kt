package org.icpclive.cds.util.serializers

import kotlinx.serialization.*
import kotlinx.serialization.builtins.serializer
import org.icpclive.cds.util.map
import kotlin.time.*

internal fun DurationUnit.asSerializer() = Long.serializer().map(
    "Duration/${this}",
    onSerialize = { it.toLong(this) },
    onDeserialize = { it.toDuration(this) }
)

public object DurationInMillisecondsSerializer : KSerializer<Duration> by DurationUnit.MILLISECONDS.asSerializer()
public object DurationInMinutesSerializer : KSerializer<Duration> by DurationUnit.MINUTES.asSerializer()
public object DurationInSecondsSerializer : KSerializer<Duration> by DurationUnit.SECONDS.asSerializer()