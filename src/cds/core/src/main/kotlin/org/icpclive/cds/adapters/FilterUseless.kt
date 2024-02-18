@file:JvmMultifileClass
@file:JvmName("Adapters")

package org.icpclive.cds.adapters

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filter
import org.icpclive.cds.*

public fun Flow<ContestState>.filterUseless(): Flow<ContestState> = filter {
    when (it.event) {
        is RunUpdate -> it.runs[it.event.newInfo.id] != it.event.newInfo
        is InfoUpdate -> it.infoBeforeEvent != it.event.newInfo
        is AnalyticsUpdate -> it.analyticsMessages[it.event.message.id] != it.event.message
    }
}