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
import kotlin.time.Duration

@OptIn(InefficientContestInfoApi::class)
@JvmName("addPreviousDaysResults")
public fun Flow<ContestUpdate>.addPreviousDays(previousDays: List<ContestState>): Flow<ContestUpdate> {
    val allProblems = mutableListOf<ProblemInfo>()
    val allRuns = mutableListOf<RunInfo>()

    fun problemId(day: Int, id: ProblemId) = "d${day + 1}.${id.value}".toProblemId()

    for ((index, state) in previousDays.withIndex()) {
        val info = state.infoAfterEvent!!
        for (problem in info.problemList.sortedBy { it.ordinal }) {
            allProblems.add(
                problem.copy(
                    ordinal = allProblems.size,
                    id = problemId(index, problem.id)
                )
            )
        }
        for (run in state.runs.values) {
            allRuns.add(
                run.copy(
                    id = "$index${'$'}${run.id}".toRunId(),
                    time = Duration.ZERO,
                    problemId = problemId(index, run.problemId),
                )
            )
        }
    }

    return transform { update ->
        when (update) {
            is AnalyticsUpdate -> emit(update)
            is InfoUpdate -> {
                val info = update.newInfo
                val newProblems = info.problemList.sortedBy { it.ordinal }.mapIndexed { index, problem ->
                    problem.copy(
                        id = problem.id,
                        ordinal = allProblems.size + index
                    )
                }
                emit(
                    InfoUpdate(
                        info.copy(
                            problemList = allProblems + newProblems
                        )
                    )
                )
                for (i in allRuns) {
                    emit(RunUpdate(i))
                }
                allRuns.clear()
            }

            is RunUpdate -> emit(update)
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