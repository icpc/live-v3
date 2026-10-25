package org.icpclive.service

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import org.icpclive.Config
import org.icpclive.api.CurrentTeamState
import org.icpclive.cds.ContestUpdate
import org.icpclive.cds.adapters.generateCommentary
import org.icpclive.cds.api.OptimismLevel
import org.icpclive.cds.scoreboard.calculateScoreboard
import org.icpclive.cds.util.*
import org.icpclive.data.Controllers
import org.icpclive.data.DataBus
import org.icpclive.service.analytics.AnalyticsGenerator

private val log by getLogger()

fun CoroutineScope.launchServices(
    loader: Flow<ContestUpdate>,
    controllers: Controllers,
    dataBus: DataBus,
) {
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
            dataBus.teamInterestingFlow.completeOrThrow(teamInterestingFlow)

            launchService(ContestStateService(dataBus.contestStateFlow))
            launchService(QueueService(dataBus.queueFlow, dataBus.queueFeaturedRunsFlow))
            launchService(ScoreboardService(dataBus::setScoreboardDiffs))
            launchService(StatisticsService(dataBus.statisticFlow))
            launchService(
                AnalyticsService(
                    controllers.advertisement,
                    controllers.tickerMessage,
                    queueFeaturedRunsFlow = dataBus.queueFeaturedRunsFlow,
                    externalActionsFlow = dataBus.analyticsActionsFlow,
                    analyticsFlow = dataBus.analyticsFlow,
                )
            )
            launchService(
                TeamSpotlightService(
                    contestStateFlow = dataBus.contestStateFlow,
                    scoreRequestFlow = dataBus.teamInterestingScoreRequestFlow,
                    socialEventsFlow = dataBus.socialEvents,
                    teamSpotlightFlow = dataBus.teamSpotlightFlow,
                    teamInteresting = teamInterestingFlow,
                )
            )
            launchService(RegularLoggingService())
            launchService(TimelineService(dataBus.timelineFlow))
        }
}
