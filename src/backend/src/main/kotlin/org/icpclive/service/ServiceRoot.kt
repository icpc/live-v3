package org.icpclive.service

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import org.icpclive.Config
import org.icpclive.cds.ContestUpdate
import org.icpclive.cds.adapters.generateCommentary
import org.icpclive.cds.api.OptimismLevel
import org.icpclive.cds.scoreboard.calculateScoreboard
import org.icpclive.cds.util.getLogger
import org.icpclive.cds.util.shareWith
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

            launchService(ContestStateService(dataBus.contestStateFlow, dataBus.contestInfoFlow))
            launchService(QueueService(dataBus.queueFeaturedRunsFlow, dataBus.queueFlow))
            launchService(ScoreboardService(dataBus::setScoreboardDiffs))
            launchService(StatisticsService(dataBus.statisticFlow))
            launchService(
                AnalyticsService(
                    controllers.advertisement,
                    controllers.tickerMessage,
                    externalActionsFlow = dataBus.analyticsActionsFlow,
                    queueFeaturedRunsFlow = dataBus.queueFeaturedRunsFlow,
                    analyticsFlow = dataBus.analyticsFlow,
                )
            )
            launchService(
                TeamSpotlightService(
                    contestInfoFlow = dataBus.contestInfoFlow,
                    scoreRequestFlow = dataBus.teamInterestingScoreRequestFlow,
                    socialEventsFlow = dataBus.socialEvents,
                    teamSpotlightFlow = dataBus.teamSpotlightFlow,
                    teamInterestingFlow = dataBus.teamInterestingFlow,
                )
            )
            launchService(RegularLoggingService())
            launchService(TimelineService(dataBus.timelineFlow))
        }
}
