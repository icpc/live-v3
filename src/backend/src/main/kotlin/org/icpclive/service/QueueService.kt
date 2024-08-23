package org.icpclive.service

import kotlinx.coroutines.*
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.*
import kotlinx.datetime.Clock
import org.icpclive.api.*
import org.icpclive.cds.*
import org.icpclive.cds.api.*
import org.icpclive.cds.api.QueueSettings
import org.icpclive.cds.scoreboard.ContestStateWithScoreboard
import org.icpclive.cds.util.*
import org.icpclive.data.DataBus
import org.icpclive.util.completeOrThrow
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

private sealed class QueueProcessTrigger
private data object Clean : QueueProcessTrigger()
private data class Event(val state: ContestState) : QueueProcessTrigger()
private data class Featured(val request: FeaturedRunAction) : QueueProcessTrigger()
private data object Subscribe : QueueProcessTrigger()

sealed class FeaturedRunAction(val runId: RunId) {
    class MakeFeatured(
        runId: RunId,
        val mediaType: MediaType,
    ) : FeaturedRunAction(runId) {
        val result: CompletableDeferred<AnalyticsCompanionRun?> = CompletableDeferred()
    }

    class MakeNotFeatured(runId: RunId) : FeaturedRunAction(runId)
}

class QueueService : Service {
    private val runs = mutableMapOf<RunId, RunInfo>()
    private val removedRuns = mutableMapOf<RunId, RunInfo>()
    private var featuredRun: FeaturedRunInfo? = null
    private val lastUpdateTime = mutableMapOf<RunId, Duration>()

    private val resultFlow = MutableSharedFlow<QueueEvent>(
        extraBufferCapacity = 100000,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )

    private val subscriberFlow = MutableStateFlow(0)

    init {
        DataBus.queueFlow.completeOrThrow(flow {
            var nothingSent = true
            resultFlow
                .onSubscription { subscriberFlow.update { it + 1 } }
                .collect {
                    val isSnapshot = it is QueueSnapshotEvent
                    if (nothingSent == isSnapshot) {
                        emit(it)
                        nothingSent = false
                    }
                }
        })
    }

    private suspend fun modifyRun(rawRun: RunInfo, sendToOverlay: Boolean = true) {
        val featuredMediaType = featuredRun?.takeIf { it.runId == rawRun.id }?.mediaType
        val run = rawRun.takeIf { it.featuredRunMedia == featuredMediaType }
            ?: rawRun.copy(featuredRunMedia = featuredMediaType)
        if (sendToOverlay) {
            resultFlow.emit(if (run.id in runs) ModifyRunInQueueEvent(run) else AddRunToQueueEvent(run))
        }
        runs[run.id] = run
    }

    private suspend fun FeaturedRunInfo.makeNotFeatured() {
        val run = runs[runId] ?: removedRuns[runId] ?: return
        featuredRun = null
        modifyRun(run.copy(featuredRunMedia = null), runId in runs)
    }

    private suspend fun removeRun(run: RunInfo) {
        runs.remove(run.id)
        featuredRun?.takeIf { it.runId == run.id }?.makeNotFeatured()
        removedRuns[run.id] = run
        resultFlow.emit(RemoveRunFromQueueEvent(run))
    }

    private fun RunResult.isFTS() = when (this) {
        is RunResult.ICPC -> isFirstToSolveRun
        is RunResult.IOI -> isFirstBestRun
        is RunResult.InProgress -> false
    }

    private fun RunInfo.getTimeInQueue(settings: QueueSettings) = when {
        featuredRunMedia != null -> settings.featuredRunWaitTime
        result.isFTS() -> settings.firstToSolveWaitTime
        result is RunResult.InProgress -> settings.inProgressRunWaitTime
        else -> settings.waitTime
    }

    private fun RunInfo.isInProgress(contestInfo: ContestInfo): Boolean {
        return result is RunResult.InProgress && contestInfo.freezeTime != null && time < contestInfo.freezeTime!!
    }


    override fun CoroutineScope.runOn(flow: Flow<ContestStateWithScoreboard>) {
        launch {
            val featuredRunsFlow = MutableSharedFlow<FeaturedRunAction>(
                extraBufferCapacity = 100,
                onBufferOverflow = BufferOverflow.DROP_OLDEST
            )
            DataBus.queueFeaturedRunsFlow.completeOrThrow(featuredRunsFlow)
            log.info { "Queue service is started" }
            var firstEventTime: Duration? = null
            val removerFlowTrigger = loopFlow(1.seconds, onError = {}) { Clean }
            val statesFlowTrigger = flow.map { Event(it.state) }
            val subscriberFlowTrigger = subscriberFlow.map { Subscribe }
            val featuredFlowTrigger = featuredRunsFlow.map { Featured(it) }
            var currentContestInfo: ContestInfo? = null
            // it's important to have all side effects after merge, as part before merge will be executed concurrently
            merge(statesFlowTrigger, removerFlowTrigger, subscriberFlowTrigger, featuredFlowTrigger).collect { event ->
                when (event) {
                    is Clean -> {
                        val info = currentContestInfo ?: return@collect
                        val currentTime = info.currentContestTime
                        runs.values
                            .filter { currentTime >= lastUpdateTime[it.id]!! + it.getTimeInQueue(info.queueSettings) }
                            .filterNot { it.featuredRunMedia != null }
                            .forEach { removeRun(it) }
                    }

                    is Event -> {
                        when (val update = event.state.lastEvent) {
                            is AnalyticsUpdate -> {}
                            is InfoUpdate -> {}
                            is RunUpdate -> {
                                val contestInfo = event.state.infoAfterEvent?.takeIf { it.status !is ContestStatus.BEFORE } ?: return@collect
                                currentContestInfo = contestInfo
                                val run = update.newInfo
                                removedRuns[run.id] = run
                                if (firstEventTime == null) {
                                    firstEventTime = contestInfo.currentContestTime
                                }
                                val runUpdateTime = contestInfo.currentContestTime.takeIf { it > firstEventTime!! + 60.seconds } ?: run.time
                                lastUpdateTime[run.id] = runUpdateTime
                                if (run.isHidden) {
                                    if (run.id in runs) {
                                        removeRun(run)
                                    }
                                } else {
                                    val shouldUpdateRun = when {
                                        // we want to update runs already in queue
                                        run.id in runs -> true
                                        // we can postpone untested run if there are too many untested runs
                                        run.isInProgress(contestInfo) && runs.values.count { it.isInProgress(contestInfo) } >= contestInfo.queueSettings.maxUntestedRun -> false
                                        // otherwise, we are adding runs if they are not too old
                                        else -> contestInfo.currentContestTime <= runUpdateTime + run.getTimeInQueue(contestInfo.queueSettings)
                                    }
                                    if (shouldUpdateRun) {
                                        modifyRun(run)
                                    }
                                }
                            }
                        }
                    }

                    is Featured -> {
                        val runId = event.request.runId
                        val run = runs[runId] ?: removedRuns[runId]
                        val info = currentContestInfo
                        if (run == null || info == null) {
                            log.warning { "There is no run with id $runId for make it featured" }
                            if (event.request is FeaturedRunAction.MakeFeatured) {
                                event.request.result.complete(null)
                            }
                            return@collect
                        }
                        when (event.request) {
                            is FeaturedRunAction.MakeFeatured -> {
                                featuredRun?.makeNotFeatured()
                                featuredRun = FeaturedRunInfo(run.id, event.request.mediaType)
                                modifyRun(run)
                                lastUpdateTime[run.id] = run.time
                                event.request.result.complete(
                                    AnalyticsCompanionRun(Clock.System.now() + info.queueSettings.featuredRunWaitTime, event.request.mediaType)
                                )
                            }

                            is FeaturedRunAction.MakeNotFeatured -> {
                                if (featuredRun?.runId == run.id) {
                                    featuredRun = null
                                    modifyRun(run, runId in runs)
                                }
                            }
                        }
                    }

                    is Subscribe -> {
                        resultFlow.emit(QueueSnapshotEvent(runs.values.sortedBy { it.time }))
                    }
                }
                val contestInfo = currentContestInfo ?: return@collect
                while (runs.size >= contestInfo.queueSettings.maxQueueSize) {
                    runs.values.asSequence()
                        .filterNot { it.result.isFTS() || it.featuredRunMedia != null }
                        .filterNot { it.isInProgress(contestInfo) }
                        .minByOrNull { lastUpdateTime[it.id]!! }
                        ?.run { removeRun(this) }
                        ?: break
                }
            }
        }
    }

    companion object {
        val log by getLogger()

        private data class FeaturedRunInfo(val runId: RunId, val mediaType: MediaType)
    }
}
