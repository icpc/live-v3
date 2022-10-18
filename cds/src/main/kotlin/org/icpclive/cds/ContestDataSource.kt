package org.icpclive.cds

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import org.icpclive.api.AnalyticsMessage
import org.icpclive.api.ContestInfo
import org.icpclive.api.RunInfo
import org.icpclive.cds.adapters.EmulationAdapter
import org.icpclive.cds.adapters.FirstToSolveAdapter
import org.icpclive.cds.clics.ClicsDataSource
import org.icpclive.cds.codeforces.CFDataSource
import org.icpclive.cds.common.RunsBufferService
import org.icpclive.cds.ejudge.EjudgeDataSource
import org.icpclive.cds.krsu.KRSUDataSource
import org.icpclive.cds.pcms.PCMSDataSource
import org.icpclive.cds.yandex.YandexDataSource
import org.icpclive.util.getLogger
import org.icpclive.util.guessDatetimeFormat
import org.icpclive.util.logAndRetryWithDelay
import java.util.*
import kotlin.time.Duration

data class ContestParseResult(
    val contestInfo: ContestInfo,
    val runs: List<RunInfo>,
    val analyticsMessages: List<AnalyticsMessage>
)

interface ContestDataSource {
    suspend fun run(
        contestInfoDeferred: CompletableDeferred<StateFlow<ContestInfo>>,
        runsDeferred: CompletableDeferred<Flow<RunInfo>>,
        analyticsMessagesDeferred: CompletableDeferred<Flow<AnalyticsMessage>>
    )
    suspend fun loadOnce(): ContestParseResult
}

abstract class FullReloadContestDataSource(val interval: Duration) : ContestDataSource {
    override suspend fun run(
        contestInfoDeferred: CompletableDeferred<StateFlow<ContestInfo>>,
        runsDeferred: CompletableDeferred<Flow<RunInfo>>,
        analyticsMessagesDeferred: CompletableDeferred<Flow<AnalyticsMessage>>
    ) {
        coroutineScope {
            val reloadFlow = flow {
                while (true) {
                    emit(loadOnce())
                    delay(interval)
                }
            }.flowOn(Dispatchers.IO)
                .logAndRetryWithDelay(interval) {
                    getLogger(ContestDataSource::class).error("Failed to reload data, retrying", it)
                }
                .stateIn(this)
            launch { RunsBufferService(reloadFlow.map { it.runs }, runsDeferred).run() }
            analyticsMessagesDeferred.complete(emptyFlow())
            contestInfoDeferred.complete(reloadFlow.map { it.contestInfo }.stateIn(this))
        }
    }
}

fun getContestDataSource(
    properties: Properties,
    creds: Map<String, String> = emptyMap(),
    calculateFTS: Boolean = true
) : ContestDataSource {
    val loader = when (val standingsType = properties.getProperty("standings.type")) {
        "CLICS" -> ClicsDataSource(properties, creds)
        "PCMS" -> PCMSDataSource(properties, creds)
        "CF" -> CFDataSource(properties, creds)
        "YANDEX" -> YandexDataSource(properties, creds)
        "EJUDGE" -> EjudgeDataSource(properties)
        "KRSU" -> KRSUDataSource(properties)
        else -> throw IllegalArgumentException("Unknown standings.type $standingsType")
    }

    val emulationSpeedProp: String? = properties.getProperty("emulation.speed")
    return if (emulationSpeedProp != null) {
        val emulationSpeed = emulationSpeedProp.toDouble()
        val emulationStartTime = guessDatetimeFormat(properties.getProperty("emulation.startTime"))
        EmulationAdapter(emulationStartTime, emulationSpeed, loader)
    } else {
        loader
    }.let { if (calculateFTS) FirstToSolveAdapter(it) else it }
}