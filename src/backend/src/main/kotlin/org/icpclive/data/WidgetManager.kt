package org.icpclive.data

import org.icpclive.api.*
import org.icpclive.cds.util.getLogger
import org.icpclive.util.completeOrThrow
import kotlin.time.Duration
import kotlin.time.TimeSource

private val logger by getLogger()

private fun WidgetUsageStatisticsEntry.Simple?.updateStatsRemove(item: Widget) =
    when {
        this == null -> {
            logger.warning { "Suspicious statistics for widget ${item.statisticsId}: removed, but wasn't added" }
            WidgetUsageStatisticsEntry.Simple(
                shownSince = null,
                shownCount = 0,
                totalShownTime = Duration.ZERO
            )
        }
        else -> {
            if (shownCount == 0) {
                logger.warning { "Suspicious statistics for widget ${item.statisticsId}: removed, but shown count was zero" }
            }
            WidgetUsageStatisticsEntry.Simple(
                shownSince = if (this.shownCount == 1) null else shownSince,
                shownCount = shownCount - 1,
                totalShownTime = totalShownTime + if (this.shownCount == 1) shownSince!!.elapsedNow() else Duration.ZERO,
            )
        }
    }

private fun WidgetUsageStatisticsEntry.Simple?.updateStatsAdd(item: Widget) : WidgetUsageStatisticsEntry.Simple =
    WidgetUsageStatisticsEntry.Simple(
        shownSince = this?.shownSince ?: TimeSource.Monotonic.markNow(),
        shownCount = (this?.shownCount ?: 0) + 1,
        totalShownTime = (this?.totalShownTime ?: Duration.ZERO)
    )


private fun WidgetUsageStatisticsEntry?.updateStatsRemove(item: Widget) : WidgetUsageStatisticsEntry = when (item) {
    is TeamViewWidget -> {
        require(this is WidgetUsageStatisticsEntry.PerTeam?)
        val res = this ?: WidgetUsageStatisticsEntry.PerTeam(mutableMapOf())
        val newEntry = (res.byTeam[item.settings.teamId] as WidgetUsageStatisticsEntry.Simple?).updateStatsRemove(item)
        WidgetUsageStatisticsEntry.PerTeam(
            res.byTeam  + (item.settings.teamId to newEntry)
        )
    }
    else -> {
        require(this is WidgetUsageStatisticsEntry.Simple?)
        updateStatsRemove(item)
    }
}

private fun WidgetUsageStatisticsEntry?.updateStatsAdd(item: Widget): WidgetUsageStatisticsEntry = when (item) {
    is TeamViewWidget -> {
        require(this is WidgetUsageStatisticsEntry.PerTeam?)
        val res = this ?: WidgetUsageStatisticsEntry.PerTeam(mutableMapOf())
        val newEntry = (res.byTeam[item.settings.teamId] as WidgetUsageStatisticsEntry.Simple?).updateStatsAdd(item)
        WidgetUsageStatisticsEntry.PerTeam(
            res.byTeam  + (item.settings.teamId to newEntry)
        )
    }
    else -> {
        require(this is WidgetUsageStatisticsEntry.Simple?)
        updateStatsAdd(item)
    }
}


class WidgetManager : ManagerWithEvents<Widget, MainScreenEvent>() {
    private val statistics = WidgetUsageStatistics(mutableMapOf())

    override fun createAddEvent(item: Widget) = ShowWidgetEvent(item)
    override fun createRemoveEvent(id: String) = HideWidgetEvent(id)
    override fun createSnapshotEvent(items: List<Widget>) = MainScreenSnapshotEvent(items)

    override fun onItemRemove(item: Widget) {
        statistics.entries[item.statisticsId] = statistics.entries[item.statisticsId].updateStatsRemove(item)
        super.onItemRemove(item)
    }

    override fun onItemAdd(item: Widget) {
        statistics.entries[item.statisticsId] = statistics.entries[item.statisticsId].updateStatsAdd(item)
        super.onItemRemove(item)
    }

    suspend fun getUsageStatistics() : WidgetUsageStatistics {
        val statisticsCopy = WidgetUsageStatistics(statistics.entries.toMutableMap())
        traverse {
            statisticsCopy.entries[it.statisticsId] = statisticsCopy.entries[it.statisticsId].updateStatsRemove(it)
        }
        return statisticsCopy
    }


    init {
        DataBus.mainScreenFlow.completeOrThrow(flow)
    }
}