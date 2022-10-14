package org.icpclive.service

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromStream
import org.icpclive.config
import org.icpclive.api.AdvancedProperties
import org.icpclive.common.util.fileChangesFlow
import org.icpclive.common.util.getLogger
import org.icpclive.common.util.suppressIfNotCancellation
import kotlin.io.path.inputStream

class AdvancedPropertiesService {
    suspend fun run(advancedPropertiesFlowDeferred: CompletableDeferred<Flow<AdvancedProperties>>) {
        val advancedPropertiesFlow = MutableStateFlow(AdvancedProperties())
        advancedPropertiesFlowDeferred.complete(advancedPropertiesFlow)
        withContext(Dispatchers.IO) {
            fileChangesFlow(config.configDirectory.resolve("advanced.json"))
                .mapNotNull { path ->
                    logger.info("Reloading $path")
                    try {
                        path.inputStream().use {
                            Json.decodeFromStream<AdvancedProperties>(it)
                        }
                    }
                    catch (e: Exception) {
                        logger.error("Failed to reload $path", e)
                        suppressIfNotCancellation(e)
                    }
                }.collect {
                    advancedPropertiesFlow.value = it
                }
        }
    }

    companion object {
        val logger = getLogger(AdvancedPropertiesService::class)
    }
}
