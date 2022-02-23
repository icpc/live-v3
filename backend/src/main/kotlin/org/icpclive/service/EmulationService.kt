package org.icpclive.service

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import org.icpclive.DataBus
import org.icpclive.api.ContestInfo
import org.icpclive.api.ContestStatus
import org.icpclive.api.RunInfo
import org.icpclive.utils.getLogger
import kotlin.random.Random
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

private class Event(val time: Duration, val process: suspend () -> Unit)

class EmulationService(
    private val startTime: Instant,
    private val emulationSpeed: Double,
    private val runs: List<RunInfo>,
    contestInfo_: ContestInfo,
    private val runsFlow: MutableSharedFlow<RunInfo>
    ) {
    val contestInfo = contestInfo_.copy(
        startTimeUnixMs = startTime.toEpochMilliseconds(),
        emulationSpeed = emulationSpeed
    )
    private val events = buildList {
        add(Event(0.seconds) {
            DataBus.contestInfoUpdates.value = contestInfo.copy(status = ContestStatus.RUNNING)}
        )
        add(Event(contestInfo.contestLengthMs.milliseconds) {
            DataBus.contestInfoUpdates.value = contestInfo.copy(status = ContestStatus.OVER)})
        for (run in runs) {
            var percentage = Random.nextDouble(0.1)
            var timeShift = 0
            if (run.isJudged) {
                do {
                    val submittedRun = run.copy(
                        percentage = percentage,
                        isJudged = false,
                        isAccepted = false,
                        isAddingPenalty = false,
                        result = "",
                    )
                    add(Event((run.time + timeShift).milliseconds) { runsFlow.emit(submittedRun) })
                    percentage += Random.nextDouble(1.0)
                    timeShift += Random.nextInt(20000)
                } while (percentage < 1.0)
            }
            add(Event((run.time + timeShift).milliseconds) { runsFlow.emit(run) })
        }
    }.sortedBy { it.time }

    suspend fun run() {
        DataBus.contestInfoUpdates.value = contestInfo.copy(status = ContestStatus.BEFORE)
        var lastLoggedTime = 0.seconds
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
        private val logger = getLogger(EmulationService::class)
    }
}