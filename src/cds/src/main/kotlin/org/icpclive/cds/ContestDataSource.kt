package org.icpclive.cds

import org.icpclive.cds.noop.NoopDataSource
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import org.icpclive.api.AnalyticsMessage
import org.icpclive.api.ContestInfo
import org.icpclive.api.RunInfo
import org.icpclive.cds.adapters.*
import org.icpclive.cds.cats.CATSDataSource
import org.icpclive.cds.clics.ClicsDataSource
import org.icpclive.cds.codeforces.CFDataSource
import org.icpclive.cds.ejudge.EjudgeDataSource
import org.icpclive.cds.krsu.KRSUDataSource
import org.icpclive.cds.pcms.PCMSDataSource
import org.icpclive.cds.yandex.YandexDataSource
import org.icpclive.util.getLogger
import org.icpclive.util.guessDatetimeFormat
import org.icpclive.util.loopFlow
import java.util.*
import kotlin.time.Duration

sealed interface ContestUpdate
data class InfoUpdate(val newInfo: ContestInfo) : ContestUpdate
data class RunUpdate(val newInfo: RunInfo) : ContestUpdate
data class Analytics(val message: AnalyticsMessage) : ContestUpdate

data class ContestParseResult(
    val contestInfo: ContestInfo,
    val runs: List<RunInfo>,
    val analyticsMessages: List<AnalyticsMessage>
)

interface ContestDataSource {
    fun getFlow(): Flow<ContestUpdate>
}

interface RawContestDataSource : ContestDataSource {
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


fun getContestDataSourceAsFlow(
    properties: Properties,
    creds: Map<String, String> = emptyMap(),
) : Flow<ContestUpdate> {
    val rawLoader = when (val standingsType = properties.getProperty("standings.type")) {
        "CLICS" -> ClicsDataSource(properties, creds)
        "PCMS" -> PCMSDataSource(properties, creds)
        "CF" -> CFDataSource(properties, creds)
        "YANDEX" -> YandexDataSource(properties, creds)
        "EJUDGE" -> EjudgeDataSource(properties)
        "KRSU" -> KRSUDataSource(properties)
        "CATS" -> CATSDataSource(properties, creds)
        "NOOP" -> NoopDataSource()
        else -> throw IllegalArgumentException("Unknown standings.type $standingsType")
    }

    val emulationSpeed = properties.getProperty("emulation.speed")?.toDouble()
    val loader = if (emulationSpeed == null) {
        rawLoader
    } else {
        val emulationStartTime = guessDatetimeFormat(properties.getProperty("emulation.startTime"))
        EmulationAdapter(emulationStartTime, emulationSpeed, rawLoader)
    }


    return loader.getFlow()
}