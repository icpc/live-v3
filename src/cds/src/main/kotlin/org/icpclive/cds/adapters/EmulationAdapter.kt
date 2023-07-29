package org.icpclive.cds.adapters

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.flow
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import org.icpclive.api.ContestStatus
import org.icpclive.cds.*
import org.icpclive.cds.common.ContestDataSource
import org.icpclive.cds.common.RawContestDataSource
import org.icpclive.util.getLogger
import org.icpclive.util.humanReadable
import kotlin.random.Random
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

internal class EmulationAdapter(
    private val startTime: Instant,
    private val emulationSpeed: Double,
    private val source: RawContestDataSource,
) : ContestDataSource {
    private class Event(val time: Duration, val process: suspend () -> Unit)

    override fun getFlow() = flow<ContestUpdate> {
        val (finalContestInfo, runs, analyticsMessages) = source.loadOnce()
        require(finalContestInfo.status == ContestStatus.OVER) { "Emulation require contest to be finished" }
        logger.info("Running in emulation mode with speed x${emulationSpeed} and startTime = ${startTime.humanReadable}")
        val contestInfo = finalContestInfo.copy(
            startTime = startTime,
            emulationSpeed = emulationSpeed
        )

        emit(InfoUpdate(contestInfo.copy(status = ContestStatus.BEFORE)))
        var lastLoggedTime = 0.seconds
        val events = buildList {
            add(Event(Duration.ZERO) {
                emit(InfoUpdate(contestInfo.copy(status = ContestStatus.RUNNING)))
            })
            add(Event(contestInfo.contestLength) {
                emit(InfoUpdate(contestInfo.copy(status = ContestStatus.OVER)))
            })
            for (run in runs) {
                var percentage = Random.nextDouble(0.1)
                var timeShift = 0
                if (run.result != null) {
                    do {
                        val submittedRun = run.copy(
                            percentage = percentage,
                            result = null
                        )
                        add(Event(run.time + timeShift.milliseconds) { emit(RunUpdate(submittedRun)) })
                        percentage += Random.nextDouble(1.0)
                        timeShift += Random.nextInt(20000)
                    } while (percentage < 1.0)
                }
                add(Event(run.time + timeShift.milliseconds) { emit(RunUpdate(run)) })
            }
            addAll(analyticsMessages.map { Event(it.relativeTime) { emit(AnalyticsUpdate(it)) } })
        }.sortedBy { it.time }
        for (event in events) {
            val nextEventTime = (startTime + event.time / emulationSpeed)
            logger.debug("Next event time = ${nextEventTime}, will sleep for ${nextEventTime - Clock.System.now()}")
            delay(nextEventTime - Clock.System.now())
            event.process()
            if (event.time - lastLoggedTime > 10.seconds) {
                logger.info("Processed events upto ${event.time}")
                lastLoggedTime = event.time
            }
        }
    }

    companion object {
        internal val logger = getLogger(EmulationAdapter::class)
    }
}