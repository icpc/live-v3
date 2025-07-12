package org.icpclive.cds.adapters.impl

import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import org.icpclive.cds.ContestUpdate
import org.icpclive.cds.InfoUpdate
import org.icpclive.cds.api.ContestInfo
import org.icpclive.cds.api.startTime
import org.icpclive.cds.tunning.TuningRule
import kotlin.time.Clock
import kotlin.time.Instant

private sealed interface AdvancedAdapterEvent {
    data class Update(val update: ContestUpdate) : AdvancedAdapterEvent
    data object Trigger : AdvancedAdapterEvent
}

internal fun applyTuningRules(flow: Flow<ContestUpdate>, advancedPropsFlow: Flow<List<TuningRule>>): Flow<ContestUpdate> {
    return flow {
        val triggerFlow = Channel<AdvancedAdapterEvent.Trigger>()
        val triggers = mutableSetOf<Instant>()
        coroutineScope {
            val advancedPropsStateFlow = advancedPropsFlow.stateIn(this)
            var contestInfo: ContestInfo? = null
            var last: ContestInfo? = null
            fun triggerAt(time: Instant) {
                if (time < Clock.System.now()) return
                if (triggers.add(time)) {
                    launch {
                        delay(time - Clock.System.now())
                        triggerFlow.send(AdvancedAdapterEvent.Trigger)
                    }
                }
            }

            suspend fun apply() {
                val ci = contestInfo ?: return
                val ap = advancedPropsStateFlow.value
                val newInfo: ContestInfo = ap.fold(ci) { acc, tuningRule ->
                    tuningRule.process(acc)
                }
                if (newInfo != last) {
                    emit(InfoUpdate(newInfo))
                    last = newInfo
                    newInfo.startTime?.let {
                        triggerAt(it)
                        triggerAt(it + newInfo.contestLength)
                    }
                }
            }
            merge(
                flow.map { AdvancedAdapterEvent.Update(it) },
                triggerFlow.receiveAsFlow().conflate(),
                advancedPropsStateFlow.map { AdvancedAdapterEvent.Trigger },
            ).collect {
                when (it) {
                    is AdvancedAdapterEvent.Trigger -> {
                        apply()
                    }

                    is AdvancedAdapterEvent.Update -> {
                        if (it.update is InfoUpdate) {
                            if (contestInfo != it.update.newInfo) {
                                contestInfo = it.update.newInfo
                                apply()
                            }
                        } else {
                            emit(it.update)
                        }
                    }
                }
            }
        }
    }
}