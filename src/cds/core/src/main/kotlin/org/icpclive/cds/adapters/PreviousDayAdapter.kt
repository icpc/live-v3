@file:JvmMultifileClass
@file:JvmName("Adapters")

package org.icpclive.cds.adapters

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromStream
import org.icpclive.cds.*
import org.icpclive.cds.api.*
import org.icpclive.cds.settings.*
import org.icpclive.cds.tunning.AdvancedProperties
import org.icpclive.util.Enumerator
import kotlin.time.Duration

@OptIn(InefficientContestInfoApi::class)
@JvmName("addPreviousDaysResults")
public fun Flow<ContestUpdate>.addPreviousDays(previousDays: List<ContestState>): Flow<ContestUpdate> {
    val teamsEnumerator = Enumerator<String>()
    val runsEnumerator = Enumerator<Pair<Int, Int>>()

    val allProblems = mutableListOf<ProblemInfo>()
    val allRuns = mutableListOf<RunInfo>()
    val teamIdToNewTeamId = mutableMapOf<Int, Int?>()
    val teamIdToCdsTeamId = mutableMapOf<Int, String>()

    fun problemId(day: Int, id: String) = if (day < previousDays.size)
        "d${day+1}.$id"
    else
        id

    for ((index, state) in previousDays.withIndex()) {
        val info = state.infoAfterEvent!!
        for (problem in info.problemList.sortedBy { it.ordinal }) {
            allProblems.add(problem.copy(
                ordinal = allProblems.size,
                id = problemId(index, problem.id)
            ))
        }
        for (run in state.runs.values) {
            allRuns.add(run.copy(
                    id = runsEnumerator[index to run.id],
                    time = Duration.ZERO,
                    problemId = problemId(index,run.problemId),
                    teamId = teamsEnumerator[info.teams[run.teamId]!!.contestSystemId].also {
                        teamIdToNewTeamId[it] = null
                        teamIdToCdsTeamId[it] = info.teams[run.teamId]!!.contestSystemId
                    }
                )
            )
        }
    }

    val runsByTeamId = allRuns.groupBy { it.teamId }

    return transform { update ->
        when (update) {
            is AnalyticsUpdate -> emit(update)
            is InfoUpdate -> {
                val info = update.newInfo
                val newProblems = info.problemList.sortedBy { it.ordinal }.mapIndexed { index, problem ->
                    problem.copy(
                        id = problemId(previousDays.size, problem.id),
                        ordinal = allProblems.size + index
                    )
                }
                val newTeamIds = info.teamList.associate { it.contestSystemId to it.id }
                val todo = buildList {
                    for ((id, oldId) in teamIdToNewTeamId.entries) {
                        val newId = newTeamIds[teamIdToCdsTeamId[id]]
                        if (oldId != newId) {
                            add(id to newId)
                        }
                    }
                }
                emit(InfoUpdate(
                    info.copy(
                        problemList = allProblems + newProblems
                    )
                ))
                for ((id, newId) in todo) {
                    teamIdToNewTeamId[id] = newId
                    val runs = runsByTeamId[id] ?: continue
                    for (run in runs) {
                        emit(RunUpdate(
                            if (newId == null) {
                                run.copy(teamId = -1, isHidden = true)
                            } else {
                                run.copy(teamId = newId)
                            }
                        ))
                    }
                }
            }
            is RunUpdate -> {
                val info = update.newInfo
                emit(RunUpdate(
                    info.copy(
                    id = runsEnumerator[previousDays.size to info.id],
                    problemId = problemId(previousDays.size, info.problemId),
                )))
            }
        }
    }
}


@JvmName("addPreviousDaysSettings")
public fun Flow<ContestUpdate>.addPreviousDays(settings: List<PreviousDaySettings>): Flow<ContestUpdate> {
    if (settings.isEmpty()) return this
    return flow {
        val results = coroutineScope {
            settings.mapIndexed { index, it ->
                async {
                    require(it.settings.emulation == null) { "Previous day can't have emulation" }
                    require(it.settings.previousDays.isEmpty()) { "Previous day can't have it's own previous days" }
                    val advancedJsonPath = it.advancedJsonPath
                    require(advancedJsonPath is UrlOrLocalPath.Local?) { "advancedJsonPath can't be url" }
                    it.settings.toFlow().let {
                        if (advancedJsonPath == null) {
                            it
                        } else {
                            val advanced = advancedJsonPath.value.toFile().inputStream().use { Json.decodeFromStream<AdvancedProperties>(it) }
                            it.applyAdvancedProperties(flowOf(advanced))
                        }
                    }.finalContestState()
                }
            }.awaitAll()
        }
        emitAll(addPreviousDays(results))
    }
}