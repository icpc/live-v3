package org.icpclive.cds.util.serializers

import kotlinx.serialization.KSerializer
import kotlinx.serialization.builtins.nullable
import org.icpclive.cds.util.map

public fun <T: Any> SingleElementListSerializer(element: KSerializer<T>): KSerializer<List<T>> = element.map(
    "SingleElementList/${element.descriptor.serialName}",
    { listOf(it) },
    {
        it.singleOrNull() ?: throw IllegalArgumentException("Expected single element, got $it")
    }
)

public fun <T: Any> SingleElementOrNullListSerializer(element: KSerializer<T>): KSerializer<List<T>> = element.nullable.map(
    "SingleElementList/${element.descriptor.serialName}",
    { listOfNotNull(it) },
    { it.singleOrNull() }
)