package org.icpclive.cds.util.serializers

import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerializationException
import kotlinx.serialization.builtins.serializer
import org.icpclive.cds.util.map

public object RegexSerializer : KSerializer<Regex> by String.serializer().map(
    "Regex",
    {
        try {
            Regex(it)
        } catch (e: Exception) {
            throw SerializationException("Failed to compile regexp: $it", e)
        }
    },
    { it.pattern }
)