package org.icpclive.cds

import kotlinx.coroutines.flow.*
import kotlinx.coroutines.runBlocking
import org.icpclive.cds.adapters.markSubmissionAfterFirstOk
import org.icpclive.cds.api.*
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.time.Duration.Companion.minutes

object AfterFirstOkTest {

    @Test
    fun simple() {
        TestData.run {
            val r = runBlocking {
                flowOf(
                    InfoUpdate(info),
                    RunUpdate(RunInfo("1".toRunId(), RunResult.ICPC(Verdict.Accepted), problemIdA, teamId1, 10.minutes, null)),
                    RunUpdate(RunInfo("2".toRunId(), RunResult.InProgress(1.0), problemIdA, teamId1, 11.minutes, null)),
                    RunUpdate(RunInfo("3".toRunId(), RunResult.ICPC(Verdict.Accepted), problemIdA, teamId1, 12.minutes, null)),
                    RunUpdate(RunInfo("4".toRunId(), RunResult.ICPC(Verdict.WrongAnswer), problemIdB, teamId1, 13.minutes, null)),
                    RunUpdate(RunInfo("5".toRunId(), RunResult.ICPC(Verdict.Accepted), problemIdB, teamId1, 14.minutes, null)),
                    RunUpdate(RunInfo("6".toRunId(), RunResult.ICPC(Verdict.WrongAnswer), problemIdA, teamId1, 15.minutes, null)),
                    RunUpdate(RunInfo("7".toRunId(), RunResult.ICPC(Verdict.Accepted), problemIdA, teamId2, 16.minutes, null)),
                ).markSubmissionAfterFirstOk()
                    .filterIsInstance<RunUpdate>()
                    .map {
                        when (val r = it.newInfo.result) {
                            is RunResult.ICPC -> r.isAfterFirstOk
                            is RunResult.IOI -> false
                            is RunResult.InProgress -> r.isAfterFirstOk
                        }
                    }
                    .toList()
            }
            assertEquals(listOf(false, true, true, false, false, true, false), r)
        }
    }

    @Test
    fun rejudge() {
        TestData.run {
            val r = runBlocking {
                flowOf(
                    InfoUpdate(info),
                    RunUpdate(RunInfo("1".toRunId(), RunResult.ICPC(Verdict.Accepted), problemIdA, teamId1, 10.minutes, null)),
                    RunUpdate(RunInfo("2".toRunId(), RunResult.ICPC(Verdict.WrongAnswer), problemIdA, teamId1, 11.minutes, null)),
                    RunUpdate(RunInfo("1".toRunId(), RunResult.ICPC(Verdict.WrongAnswer), problemIdA, teamId1, 12.minutes, null)),
                ).markSubmissionAfterFirstOk()
                    .filterIsInstance<RunUpdate>()
                    .map {
                        it.newInfo.id.value to when (val r = it.newInfo.result) {
                            is RunResult.ICPC -> r.isAfterFirstOk
                            is RunResult.IOI -> false
                            is RunResult.InProgress -> r.isAfterFirstOk
                        }
                    }
                    .toList()
            }
            assertEquals(listOf(
                "1" to false,
                "2" to true,
                "2" to false,
                "1" to false,
            ), r)
        }

    }
}