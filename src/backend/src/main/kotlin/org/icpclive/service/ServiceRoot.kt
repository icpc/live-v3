package org.icpclive.service

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import org.icpclive.Config
import org.icpclive.api.CurrentTeamState
import org.icpclive.cds.ContestUpdate
import org.icpclive.cds.adapters.generateCommentary
import org.icpclive.cds.api.OptimismLevel
import org.icpclive.cds.scoreboard.calculateScoreboard
import org.icpclive.cds.util.completeOrThrow
import org.icpclive.cds.util.getLogger
import org.icpclive.cds.util.shareWith
import org.icpclive.data.DataBus
import org.icpclive.service.analytics.AnalyticsGenerator

private val log by getLogger()

fun CoroutineScope.launchServices(loader: Flow<ContestUpdate>) {
    val commentaryGenerator = AnalyticsGenerator(Config.analyticsTemplatesFile)
    loader
        .calculateScoreboard(OptimismLevel.NORMAL)
        .generateCommentary(commentaryGenerator::getMessages)
        .buffer(Int.MAX_VALUE)
        .onStart { log.info { "Start loading data" } }
        .shareWith(this) {
            fun CoroutineScope.launchService(service: Service) = withSubscription {
                launch(CoroutineName(service::class.simpleName!!)) {
                    with(service) {
                        this@launch.runOn(it.onStart {
                            log.info { "Service ${service::class.simpleName} subscribed to cds data" }
                        })
                    }
                }
            }

            val teamInterestingFlow = MutableStateFlow(emptyList<CurrentTeamState>())
            DataBus.teamInterestingFlow.completeOrThrow(teamInterestingFlow)

            launchService(ContestStateService())
            launchService(QueueService())
            launchService(ScoreboardService())
            launchService(StatisticsService())
            launchService(AnalyticsService())
            launchService(TeamSpotlightService(teamInteresting = teamInterestingFlow))
            launchService(RegularLoggingService())
            launchService(TimelineService())
        }
}
