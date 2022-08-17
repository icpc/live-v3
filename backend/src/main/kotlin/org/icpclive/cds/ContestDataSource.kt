package org.icpclive.cds

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import org.icpclive.api.AnalyticsMessage
import org.icpclive.api.ContestInfo
import org.icpclive.api.RunInfo
import org.icpclive.cds.clics.ClicsDataSource
import org.icpclive.cds.codeforces.CFDataSource
import org.icpclive.cds.ejudge.EjudgeDataSource
import org.icpclive.cds.pcms.PCMSDataSource
import org.icpclive.cds.yandex.YandexDataSource
import org.icpclive.config
import org.icpclive.service.launchEmulation
import org.icpclive.utils.guessDatetimeFormat
import java.io.FileInputStream
import java.io.FileNotFoundException
import java.nio.file.Files
import java.util.*

interface ContestDataSource {
    suspend fun run()
    suspend fun loadOnce(): ContestParseResult
}

data class ContestParseResult(
    val contestInfo: ContestInfo,
    val runs: List<RunInfo>,
    val analyticsMessages: List<AnalyticsMessage> = emptyList()
)

fun CoroutineScope.launchContestDataSource() {
    val path = config.configDirectory.resolve("events.properties")
    if (!Files.exists(path)) throw FileNotFoundException("events.properties not found in ${config.configDirectory}")
    val properties = Properties()
    FileInputStream(path.toString()).use { properties.load(it) }

    val loader: ContestDataSource = when (val standingsType = properties.getProperty("standings.type")) {
        "CLICS" -> ::ClicsDataSource
        "PCMS" -> ::PCMSDataSource
        "CF" -> ::CFDataSource
        "YANDEX" -> ::YandexDataSource
        "EJUDGE" -> ::EjudgeDataSource
        else -> throw IllegalArgumentException("Unknown standings.type $standingsType")
    }(properties)

    launch {
        val emulationSpeedProp: String? = properties.getProperty("emulation.speed")
        if (emulationSpeedProp != null) {
            coroutineScope {
                val emulationSpeed = emulationSpeedProp.toDouble()
                val emulationStartTime = guessDatetimeFormat(properties.getProperty("emulation.startTime"))
                val (contestInfo, submissions, analyticsEvents) = loader.loadOnce()
                launchEmulation(
                    emulationStartTime, emulationSpeed,
                    submissions,
                    contestInfo,
                    analyticsEvents
                )
            }
        } else {
            loader.run()
        }
    }
}
