package org.icpclive.cds

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import org.icpclive.api.ContestInfo
import org.icpclive.api.RunInfo
import org.icpclive.cds.clics.ClicsDataSource
import org.icpclive.cds.codeforces.CFDataSource
import org.icpclive.cds.ejudge.EjudgeDataSource
import org.icpclive.cds.pcms.PCMSDataSource
import org.icpclive.cds.yandex.YandexDataSource
import org.icpclive.config.Config.loadProperties
import org.icpclive.service.launchEmulation
import org.icpclive.utils.guessDatetimeFormat

interface ContestDataSource {
    suspend fun run()
    suspend fun loadOnce(): Pair<ContestInfo, List<RunInfo>>
}

fun CoroutineScope.launchContestDataSource() {
    val properties = loadProperties("events")
    val loader: ContestDataSource = when (val standingsType = properties.getProperty("standings.type")) {
        "CLICS" -> ClicsDataSource()
        "PCMS" -> PCMSDataSource(properties)
        "CF" -> CFDataSource()
        "YANDEX" -> YandexDataSource()
        "EJUDGE" -> EjudgeDataSource()
        else -> throw IllegalArgumentException("Unknown standings.type $standingsType")
    }

    launch {
        val emulationSpeedProp: String? = properties.getProperty("emulation.speed")
        if (emulationSpeedProp != null) {
            coroutineScope {
                val emulationSpeed = emulationSpeedProp.toDouble()
                val emulationStartTime = guessDatetimeFormat(properties.getProperty("emulation.startTime"))
                val (contestInfo, submissions) = loader.loadOnce()
                launchEmulation(
                    emulationStartTime, emulationSpeed,
                    submissions,
                    contestInfo
                )
            }
        } else {
            loader.run()
        }
    }
}
