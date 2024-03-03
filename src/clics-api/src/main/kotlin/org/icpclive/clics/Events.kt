package org.icpclive.clics

import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.modules.SerializersModule
import org.icpclive.util.postProcess

public fun clicsEventsSerializersModule(
    feedVersion: FeedVersion,
    urlPostprocessor: (String) -> Url = { Url(it) },
): SerializersModule = SerializersModule {
    include(
        when (feedVersion) {
            FeedVersion.`2020_03` -> org.icpclive.clics.v202003.serializersModule()
            FeedVersion.`2022_07` -> org.icpclive.clics.v202207.serializersModule()
            FeedVersion.`2023_06` -> org.icpclive.clics.v202306.serializersModule()
        }
    )
    postProcess(
        String.serializer(),
        onSerialize = { it: Url -> it.value },
        onDeserialize = urlPostprocessor
    )
}
