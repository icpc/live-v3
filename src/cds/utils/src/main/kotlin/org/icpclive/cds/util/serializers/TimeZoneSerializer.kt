package org.icpclive.cds.util.serializers

import kotlinx.datetime.TimeZone
import kotlinx.serialization.serializer
import org.icpclive.cds.util.DelegatedSerializer

public object TimeZoneSerializer : DelegatedSerializer<TimeZone, String>("TimeZone", serializer()) {
    override fun onDeserialize(value: String): TimeZone = TimeZone.of(value)
    override fun onSerialize(value: TimeZone): String = value.id
}