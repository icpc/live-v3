package org.icpclive.service

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromStream
import org.icpclive.config
import org.icpclive.api.AdvancedProperties
import org.icpclive.utils.fileChangesFlow
import org.icpclive.utils.getLogger
import kotlin.io.path.inputStream

class AdvancedPropertiesService {
    suspend fun run(advancedPropertiesFlow: MutableStateFlow<AdvancedProperties>) {
        withContext(Dispatchers.IO) {
            fileChangesFlow(config.configDirectory.resolve("advanced.json"))
                .mapNotNull { path ->
                    logger.info("Reloading $path")
                    try {
                        path.inputStream().use {
                            Json.decodeFromStream<AdvancedProperties>(it)
                        }
                    } catch (e: Exception) {
                        logger.error("Failed to reload $path", e)
                        null
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
