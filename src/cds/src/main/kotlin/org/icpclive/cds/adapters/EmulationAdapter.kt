package org.icpclive.cds.adapters

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import org.icpclive.api.AnalyticsMessage
import org.icpclive.api.ContestInfo
import org.icpclive.api.ContestStatus
import org.icpclive.api.RunInfo
import org.icpclive.cds.ContestDataSource
import org.icpclive.cds.RawContestDataSource
import org.icpclive.util.getLogger
import org.icpclive.util.humanReadable
import org.icpclive.util.reliableSharedFlow
import kotlin.random.Random
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

class EmulationAdapter(
    private val startTime: Instant,
    private val emulationSpeed: Double,
    private val source: RawContestDataSource,
) : ContestDataSource {
    private class Event(val time: Duration, val process: suspend () -> Unit)

    override suspend fun run(
        contestInfoDeferred: CompletableDeferred<StateFlow<ContestInfo>>,
        runsDeferred: CompletableDeferred<Flow<RunInfo>>,
        analyticsMessagesDeferred: CompletableDeferred<Flow<AnalyticsMessage>>
    ) {
        val (finalContestInfo, runs, analyticsMessages) = source.loadOnce()
        require(finalContestInfo.status == ContestStatus.OVER) { "Emulation require contest to be finished" }
        logger.info("Running in emulation mode with speed x${emulationSpeed} and startTime = ${startTime.humanReadable}")
        val contestInfo = finalContestInfo.copy(
            startTime = startTime,
            emulationSpeed = emulationSpeed
        )

        val runsFlow = reliableSharedFlow<RunInfo>()
        val contestInfoFlow = MutableStateFlow(contestInfo)
        val analyticsEventsFlow = reliableSharedFlow<AnalyticsMessage>()
        contestInfoFlow.value = contestInfo.copy(status = ContestStatus.BEFORE)
        contestInfoDeferred.complete(contestInfoFlow)
        runsDeferred.complete(runsFlow)
        analyticsMessagesDeferred.complete(analyticsEventsFlow)
        var lastLoggedTime = 0.seconds
        val events = buildList {
            add(Event(Duration.ZERO) {
                contestInfoFlow.value = contestInfo.copy(status = ContestStatus.RUNNING)
            })
            add(Event(contestInfo.contestLength) {
                contestInfoFlow.value = contestInfo.copy(status = ContestStatus.OVER)
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
                        add(Event(run.time + timeShift.milliseconds) { runsFlow.emit(submittedRun) })
                        percentage += Random.nextDouble(1.0)
                        timeShift += Random.nextInt(20000)
                    } while (percentage < 1.0)
                }
                add(Event(run.time + timeShift.milliseconds) { runsFlow.emit(run) })
            }
            addAll(analyticsMessages.map { Event(it.relativeTime) { analyticsEventsFlow.emit(it) } })
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