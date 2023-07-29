package org.icpclive.cds

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import org.icpclive.api.AnalyticsMessage
import org.icpclive.api.ContestInfo
import org.icpclive.api.RunInfo
import org.icpclive.cds.adapters.*
import org.icpclive.util.getLogger
import org.icpclive.util.loopFlow
import kotlin.time.Duration
import kotlinx.serialization.properties.*
import kotlinx.serialization.properties.Properties

sealed interface ContestUpdate
data class InfoUpdate(val newInfo: ContestInfo) : ContestUpdate
data class RunUpdate(val newInfo: RunInfo) : ContestUpdate
data class Analytics(val message: AnalyticsMessage) : ContestUpdate

@Serializable
data class ContestParseResult(
    val contestInfo: ContestInfo,
    val runs: List<RunInfo>,
    val analyticsMessages: List<AnalyticsMessage>
)

interface ContestDataSource {
    fun getFlow(): Flow<ContestUpdate>
}

internal interface RawContestDataSource : ContestDataSource {
    suspend fun loadOnce(): ContestParseResult
}

abstract class FullReloadContestDataSource(val interval: Duration) : RawContestDataSource {
    override fun getFlow() = flow {
        loopFlow(
            interval,
            { getLogger(ContestDataSource::class).error("Failed to reload data, retrying", it) }
        ) {
            loadOnce()
        }.flowOn(Dispatchers.IO)
            .collect {
                emit(InfoUpdate(it.contestInfo))
                it.runs.forEach { emit(RunUpdate(it)) }
                it.analyticsMessages.forEach { emit(Analytics(it)) }
            }
    }
}

@OptIn(ExperimentalSerializationApi::class)
fun getContestDataSourceAsFlow(
    properties: java.util.Properties,
    creds: Map<String, String> = emptyMap(),
) : Flow<ContestUpdate> {
    properties.getProperty("standings.type")?.let { properties.setProperty("type", it.lowercase()) }
    properties.getProperty("standings.resultType")?.let { properties.setProperty("resultType", it.uppercase()) }
    @Suppress("UNCHECKED_CAST")
    val settings = Properties.decodeFromStringMap<CDSSettings>(properties as Map<String, String>)
    val rawLoader = settings.toDataSource(creds)

    val loader = when (val emulationSettings = settings.emulation) {
        null -> rawLoader
        else -> EmulationAdapter(emulationSettings.startTime, emulationSettings.speed, rawLoader)
    }

    return loader.getFlow()
}

