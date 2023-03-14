package org.icpclive.service

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.*
import org.icpclive.config
import org.icpclive.api.AdvancedProperties
import org.icpclive.util.fileJsonContentFlow
import org.icpclive.util.getLogger

class AdvancedPropertiesService {
    suspend fun run(advancedPropertiesFlowDeferred: CompletableDeferred<Flow<AdvancedProperties>>) {
        coroutineScope {
            val advancedJsonPath = config.configDirectory.resolve("advanced.json")
            advancedPropertiesFlowDeferred.complete(
                fileJsonContentFlow<AdvancedProperties>(advancedJsonPath, logger)
                    .stateIn(this, SharingStarted.Eagerly, AdvancedProperties())
            )
        }
    }

    companion object {
        val logger = getLogger(AdvancedPropertiesService::class)
    }
}
