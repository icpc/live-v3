package org.icpclive.clics

import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.modules.SerializersModule
import org.icpclive.cds.util.postProcess
import org.icpclive.clics.events.EventToken

public fun clicsEventsSerializersModule(
    feedVersion: FeedVersion,
    tokenPrefix: String,
    urlPostprocessor: (String) -> Url = { Url(it) },
): SerializersModule = SerializersModule {
    include(
        when (feedVersion) {
            FeedVersion.`2020_03` -> org.icpclive.clics.v202003.serializersModule()
            FeedVersion.`2022_07` -> org.icpclive.clics.v202207.serializersModule()
            FeedVersion.`2023_06` -> org.icpclive.clics.v202306.serializersModule()
            FeedVersion.DRAFT -> org.icpclive.clics.vDRAFT.serializersModule()
        }
    )
    postProcess(
        String.serializer(),
        onSerialize = { it: EventToken -> it.value.removePrefix(tokenPrefix) },
        onDeserialize = { EventToken(tokenPrefix + it) }
    )
    postProcess(
        String.serializer(),
        onSerialize = { it: Url -> it.value },
        onDeserialize = urlPostprocessor
    )
}
