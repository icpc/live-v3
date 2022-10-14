package org.icpclive.service

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.datetime.Clock
import org.icpclive.api.*
import org.junit.Assert
import org.junit.Test
import java.awt.Color
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

class TeamSpotlightServiceTest {
    private val simpleProblems = listOf(
        ProblemInfo("A", "Problem A", Color.BLACK, 1, 1),
        ProblemInfo("B", "Problem B", Color.BLACK, 2, 2)
    )
    private val simpleTeams = (1..10)
        .map { TeamInfo(it, "Team $it", "Team $it", "$it", emptyList(), null, emptyMap()) }
    private val simpleContestInfo =
        ContestInfo(ContestStatus.RUNNING, Clock.System.now(), 1.hours, 1.hours, simpleProblems, simpleTeams)

    var runIdCounter = 1
    private fun run(
        teamId: Int,
        problemId: Int,
        isAC: Boolean = true,
        isFts: Boolean = false,
        isJudged: Boolean = true,
        time: kotlin.time.Duration = 0.minutes
    ) =
        RunInfo(runIdCounter++, isAC, isJudged, !isAC, "", problemId, teamId, 1.0, time, isFts)

    private class SimpleTestContext(
        val infoFlow: MutableStateFlow<ContestInfo>,
        val runFlow: MutableSharedFlow<RunInfo>,
        val service: TeamSpotlightService,
        val serviceScope: CoroutineScope,
        val blockingScope: CoroutineScope,
    ) {
        fun launch(block: suspend CoroutineScope.() -> Unit) {
            blockingScope.launch { block() }
        }
    }

    @OptIn(DelicateCoroutinesApi::class)
    private fun simpleServiceTest(block: SimpleTestContext.() -> Unit) {
        val infoFlow = MutableStateFlow(simpleContestInfo)
        val runFlow = MutableSharedFlow<RunInfo>()
        val scoreboardFlow = MutableStateFlow(Scoreboard(emptyList()))
        runBlocking {
            val serviceScope = CoroutineScope(newSingleThreadContext("TeamAccentService"))
            val service = TeamSpotlightService()
            serviceScope.launch { service.run(infoFlow, runFlow,scoreboardFlow) }
            block(SimpleTestContext(infoFlow, runFlow, service, serviceScope, this))
        }
    }

    @Test
    fun serviceUseRunPriority() {
        simpleServiceTest {
            launch {
                delay(0.1.seconds)
                runFlow.emit(run(1, 1, isFts = true))
                delay(0.3.seconds)
                runFlow.emit(run(2, 1, isAC = false))
                runFlow.emit(run(3, 1))
                runFlow.emit(run(4, 2, isFts = true))
            }
            launch {
                val keyTeams = service.getFlow().take(4).toList()
                serviceScope.cancel()
                Assert.assertEquals(1, keyTeams[0].teamId)
                Assert.assertEquals(4, keyTeams[1].teamId)
                Assert.assertEquals(3, keyTeams[2].teamId)
                Assert.assertEquals(2, keyTeams[3].teamId)
            }
        }
    }

    @Test
    fun serviceIgnoreNoJudgedRun() {
        simpleServiceTest {
            launch {
                delay(0.1.seconds)
                runFlow.emit(run(1, 2, isFts = true))
                delay(0.3.seconds)
                runFlow.emit(run(2, 1, isJudged = false))
                runFlow.emit(run(3, 1, isJudged = false))
                runFlow.emit(run(4, 2, isJudged = false))
                runFlow.emit(run(5, 2, isAC = false))
                runFlow.emit(run(6, 2))
            }
            launch {
                val keyTeams = service.getFlow().take(3).toList()
                serviceScope.cancel()
                Assert.assertEquals(1, keyTeams[0].teamId)
                Assert.assertEquals(6, keyTeams[1].teamId)
                Assert.assertEquals(5, keyTeams[2].teamId)
            }
        }
    }
}
