package org.icpclive.cds.util.serializers

import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import org.icpclive.cds.util.map

public fun <T> SingleElementListSerializer(element: KSerializer<T>): KSerializer<List<T>> = element.map(
    "SingleElementList/${element.descriptor.serialName}",
    { listOf(it) },
    {
        it.singleOrNull() ?: throw IllegalArgumentException("Expected single element, got $it")
    }
)