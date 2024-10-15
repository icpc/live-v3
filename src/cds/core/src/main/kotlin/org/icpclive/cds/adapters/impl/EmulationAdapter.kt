package org.icpclive.cds.adapters.impl

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import org.icpclive.cds.*
import org.icpclive.cds.adapters.finalContestState
import org.icpclive.cds.api.*
import org.icpclive.cds.settings.EmulationSettings
import org.icpclive.cds.util.serializers.HumanTimeSerializer
import org.icpclive.cds.util.getLogger
import kotlin.random.Random
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

private val logger by getLogger()

internal fun toEmulationFlow(flow: Flow<ContestUpdate>, emulationSettings: EmulationSettings) = flow {
    val scope = CoroutineScope(currentCoroutineContext())
    val waitProgress = MutableStateFlow<Pair<ContestStatus, Int>?>(null)
    val logJob = scope.launch {
        while (true) {
            delay(1.seconds)
            val r = waitProgress.value
            logger.info { "Waiting for contest to become Finalized to start emulation. Current status is ${r?.first}. ${r?.second} runs are still in progress." }
        }
    }
    val state = flow.finalContestState { state, count ->
        if (state != null) {
            waitProgress.value = state to count
        }
    }
    logJob.cancel()
    val finalContestInfo = state.infoAfterEvent!!
    val runs = state.runsAfterEvent.values.toList()
    val commentaryMessages = state.commentaryMessagesAfterEvent.values.toList()
    logger.info { "Running in emulation mode with speed x${emulationSettings.speed} and startTime = ${HumanTimeSerializer.format(emulationSettings.startTime)}" }
    val contestInfo = finalContestInfo.copy(
        emulationSpeed = emulationSettings.speed
    )

    this.emit(-Duration.INFINITE to InfoUpdate(contestInfo.copy(status = ContestStatus.BEFORE(scheduledStartAt = emulationSettings.startTime))))
    buildList {
        this.add(
            Duration.ZERO to InfoUpdate(
                contestInfo.copy(
                    status = ContestStatus.RUNNING(startedAt = emulationSettings.startTime)
                )
            )
        )
        if (contestInfo.freezeTime != null && contestInfo.freezeTime < contestInfo.contestLength) {
            this.add(
                contestInfo.freezeTime to InfoUpdate(
                    contestInfo.copy(
                        status = ContestStatus.RUNNING(
                            startedAt = emulationSettings.startTime,
                            frozenAt = emulationSettings.startTime + contestInfo.freezeTime / emulationSettings.speed
                        )
                    )
                )
            )
        }
        this.add(
            contestInfo.contestLength to InfoUpdate(
                contestInfo.copy(
                    status = ContestStatus.OVER(
                        startedAt = emulationSettings.startTime,
                        finishedAt = emulationSettings.startTime + contestInfo.contestLength / emulationSettings.speed,
                        frozenAt = if (contestInfo.freezeTime != null) emulationSettings.startTime + contestInfo.freezeTime / emulationSettings.speed else null,
                    )
                )
            )
        )
        for (run in runs) {
            var percentage = Random.nextDouble(0.1)
            var timeShift = 0
            if (run.result !is RunResult.InProgress && run.time > Duration.ZERO && emulationSettings.useRandomInProgress) {
                do {
                    val submittedRun = run.copy(
                        result = RunResult.InProgress(percentage)
                    )
                    this.add((run.time + timeShift.milliseconds) to RunUpdate(submittedRun))
                    percentage += Random.nextDouble(1.0)
                    timeShift += Random.nextInt(20000)
                } while (percentage < 1.0)
            }
            this.add((run.time + timeShift.milliseconds) to RunUpdate(run))
        }
        this.addAll(commentaryMessages.map { it.relativeTime to CommentaryMessagesUpdate(it.copy(time = emulationSettings.startTime + it.relativeTime / emulationSettings.speed)) })
    }.sortedBy { it.first }.forEach { this.emit(it) }
}.map { (emulationSettings.startTime + it.first / emulationSettings.speed) to it.second }
    .toTimedFlow()


private fun <T> Flow<Pair<Instant, T>>.toTimedFlow(): Flow<T> {
    return map { (nextEventTime, item) ->
        delay(nextEventTime - Clock.System.now())
        item
    }
}
