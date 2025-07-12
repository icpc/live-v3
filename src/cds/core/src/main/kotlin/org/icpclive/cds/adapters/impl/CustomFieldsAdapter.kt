package org.icpclive.cds.adapters.impl

import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.*
import org.icpclive.cds.*
import org.icpclive.cds.api.*
import org.icpclive.cds.util.getLogger

private val logger by getLogger()

private sealed interface CustomFieldsAdapterEvent {
    data class Update(val update: ContestUpdate) : CustomFieldsAdapterEvent
    data object Trigger : CustomFieldsAdapterEvent
}

@OptIn(InefficientContestInfoApi::class)
private fun applyCustomFieldsMap(ci: ContestInfo, cf: Map<TeamId, Map<String, String>>) : ContestInfo {
    if (cf.isEmpty()) return ci
    val unknownTeams = cf.keys.filterNot { it in ci.teams }
    if (unknownTeams.isNotEmpty()) {
        logger.warning { "Unknown teams in custom fields csv: $unknownTeams" }
    }
    return ci.copy(
        teamList = ci.teamList.map { team -> team.copy(customFields = (cf[team.id] ?: emptyMap()) + team.customFields) }
    )
}

internal fun applyCustomFieldsMap(flow: Flow<ContestUpdate>, customFieldsFlow: Flow<Map<TeamId, Map<String, String>>>): Flow<ContestUpdate> =
    flow {
        coroutineScope {
            val customFieldStateFlow = customFieldsFlow.stateIn(this)
            var contestInfo: ContestInfo? = null

            suspend fun apply() {
                val ci = contestInfo ?: return
                val cf = customFieldStateFlow.value
                emit(InfoUpdate(applyCustomFieldsMap(ci, cf)))
            }
            merge(
                flow.map { CustomFieldsAdapterEvent.Update(it) },
                customFieldStateFlow.map { CustomFieldsAdapterEvent.Trigger },
            ).collect {
                when (it) {
                    is CustomFieldsAdapterEvent.Trigger -> {
                        apply()
                    }

                    is CustomFieldsAdapterEvent.Update -> {
                        when (it.update) {
                            is InfoUpdate -> {
                                if (contestInfo != it.update.newInfo) {
                                    contestInfo = it.update.newInfo
                                    apply()
                                }
                            }

                            is RunUpdate -> {
                                emit(it.update)
                            }

                            else -> {
                                emit(it.update)
                            }
                        }
                    }
                }
            }
        }
    }


