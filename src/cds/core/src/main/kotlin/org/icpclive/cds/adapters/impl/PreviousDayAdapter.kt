package org.icpclive.cds.adapters.impl

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromStream
import org.icpclive.cds.*
import org.icpclive.cds.adapters.finalContestState
import org.icpclive.cds.api.*
import org.icpclive.cds.settings.*
import org.icpclive.cds.tunning.AdvancedProperties
import kotlin.time.Duration

@OptIn(InefficientContestInfoApi::class)
@JvmName("addPreviousDaysResults")
internal fun addPreviousDays(flow: Flow<ContestUpdate>, previousDays: List<ContestState>): Flow<ContestUpdate> {
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
        for (run in state.runsAfterEvent.values) {
            allRuns.add(
                run.copy(
                    id = "$index${'$'}${run.id}".toRunId(),
                    time = Duration.ZERO,
                    problemId = problemId(index, run.problemId),
                )
            )
        }
    }

    return flow.transform { update ->
        when (update) {
            is CommentaryMessagesUpdate -> this.emit(update)
            is InfoUpdate -> {
                val info = update.newInfo
                val newProblems = info.problemList.sortedBy { it.ordinal }.mapIndexed { index, problem ->
                    problem.copy(
                        id = problem.id,
                        ordinal = allProblems.size + index
                    )
                }
                this.emit(
                    InfoUpdate(
                        info.copy(
                            problemList = allProblems + newProblems
                        )
                    )
                )
                for (i in allRuns) {
                    this.emit(RunUpdate(i))
                }
                allRuns.clear()
            }

            is RunUpdate -> this.emit(update)
        }
    }
}


@JvmName("addPreviousDaysSettings")
internal fun addPreviousDays(flow: Flow<ContestUpdate>, settings: List<PreviousDaySettings>): Flow<ContestUpdate> {
    if (settings.isEmpty()) return flow
    return flow {
        val results = coroutineScope {
            settings.mapIndexed { index, it ->
                async {
                    require(it.settings.emulation == null) { "Previous day can't have emulation" }
                    require(it.settings.previousDays.isEmpty()) { "Previous day can't have it's own previous days" }
                    val advancedJsonPath = it.advancedJsonPath
                    it.settings.toFlow().let {
                        if (advancedJsonPath == null) {
                            it
                        } else {
                            val advanced = advancedJsonPath.value.toFile().inputStream().use { Json.decodeFromStream<AdvancedProperties>(it) }
                            applyAdvancedProperties(it, flowOf(advanced))
                        }
                    }.finalContestState()
                }
            }.awaitAll()
        }
        emitAll(addPreviousDays(flow, results))
    }
}
