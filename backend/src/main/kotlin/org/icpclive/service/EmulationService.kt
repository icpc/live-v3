package org.icpclive.service

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import org.icpclive.api.AnalyticsEvent
import org.icpclive.api.ContestInfo
import org.icpclive.api.ContestStatus
import org.icpclive.api.RunInfo
import org.icpclive.utils.getLogger
import org.icpclive.utils.humanReadable
import org.icpclive.utils.reliableSharedFlow
import kotlin.random.Random
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

private class Event(val time: Duration, val process: suspend () -> Unit)

class EmulationService(
    private val startTime: Instant,
    private val emulationSpeed: Double,
    private val runs: List<RunInfo>,
    private val analyticsEvents: List<AnalyticsEvent>,
    private val contestInfoFlow: MutableStateFlow<ContestInfo>,
    private val runsFlow: MutableSharedFlow<RunInfo>,
    private val analyticsEventsFlow: MutableSharedFlow<AnalyticsEvent>
) {
    val contestInfo = contestInfoFlow.value.copy(
        startTime = startTime,
        emulationSpeed = emulationSpeed
    )
    private val events = buildList {
        add(Event(Duration.ZERO) {
            contestInfoFlow.value = contestInfo.copy(status = ContestStatus.RUNNING)
        })
        add(Event(contestInfo.contestLength) {
            contestInfoFlow.value = contestInfo.copy(status = ContestStatus.OVER)
        })
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
                    add(Event(run.time + timeShift.milliseconds) { runsFlow.emit(submittedRun) })
                    percentage += Random.nextDouble(1.0)
                    timeShift += Random.nextInt(20000)
                } while (percentage < 1.0)
            }
            add(Event(run.time + timeShift.milliseconds) { runsFlow.emit(run) })
        }
        addAll(analyticsEvents.map { Event(it.relativeTime) { analyticsEventsFlow.emit(it) } })
    }.sortedBy { it.time }

    suspend fun run() {
        contestInfoFlow.value = contestInfo.copy(status = ContestStatus.BEFORE)
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
        internal val logger = getLogger(EmulationService::class)
    }
}

fun CoroutineScope.launchEmulation(
    startTime: Instant,
    speed: Double,
    runs: List<RunInfo>,
    contestInfo: ContestInfo,
    analyticsEvents: List<AnalyticsEvent> = emptyList()
) {
    EmulationService.logger.info("Running in emulation mode with speed x${speed} and startTime = ${startTime.humanReadable}")
    val rawRunsFlow = reliableSharedFlow<RunInfo>()
    val contestInfoFlow = MutableStateFlow(contestInfo)
    val analyticsEventsFlow = reliableSharedFlow<AnalyticsEvent>()
    launch {
        EmulationService(
            startTime,
            speed,
            runs,
            analyticsEvents,
            contestInfoFlow,
            rawRunsFlow,
            analyticsEventsFlow
        ).run()
    }
    launchICPCServices(rawRunsFlow, contestInfoFlow, analyticsEventsFlow)
}
