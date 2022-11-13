package org.icpclive.service

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.*
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromStream
import org.icpclive.config
import org.icpclive.api.AdvancedProperties
import org.icpclive.util.fileChangesFlow
import org.icpclive.util.getLogger
import org.icpclive.util.logAndRetryWithDelay
import kotlin.io.path.inputStream
import kotlin.time.Duration.Companion.seconds

class AdvancedPropertiesService {
    suspend fun run(advancedPropertiesFlowDeferred: CompletableDeferred<Flow<AdvancedProperties>>) {
        coroutineScope {
            val advancedJsonPath = config.configDirectory.resolve("advanced.json")
            advancedPropertiesFlowDeferred.complete(
                fileChangesFlow(advancedJsonPath)
                    .mapNotNull { path ->
                        logger.info("Reloading $path")
                        path.inputStream().use {
                            Json.decodeFromStream<AdvancedProperties>(it)
                        }
                    }.flowOn(Dispatchers.IO)
                    .logAndRetryWithDelay(5.seconds) {
                        logger.error("Failed to reload advanced.json", it)
                    }
                    .stateIn(this, SharingStarted.Eagerly, AdvancedProperties())
            )
        }
    }

    companion object {
        val logger = getLogger(AdvancedPropertiesService::class)
    }
}
