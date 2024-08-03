package org.icpclive.cds.adapters

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.icpclive.cds.ContestUpdate
import org.icpclive.cds.api.*
import org.icpclive.cds.utils.withGroupedRuns

private interface ScoreAccumulator {
    fun add(score: RunResult.IOI)
    val total: Double
}

private class MaxByGroupScoreAccumulator : ScoreAccumulator {
    private val bestByGroup = mutableMapOf<Int, Double>()
    override var total = 0.0

    override fun add(score: RunResult.IOI) {
        val byGroup = score.score
        for (g in byGroup.indices) {
            if (bestByGroup.getOrDefault(g, 0.0) < byGroup[g]) {
                total += byGroup[g] - bestByGroup.getOrDefault(g, 0.0)
                bestByGroup[g] = byGroup[g]
            }
        }
    }
}

private class MaxTotalScoreAccumulator : ScoreAccumulator {
    override var total = 0.0

    override fun add(score: RunResult.IOI) {
        total = maxOf(total, score.score.sum())
    }
}

private class LastScoreAccumulator : ScoreAccumulator {
    override var total = 0.0

    override fun add(score: RunResult.IOI) {
        total = score.score.sum()
    }
}

private class LastOKScoreAccumulator : ScoreAccumulator {
    override var total = 0.0

    override fun add(score: RunResult.IOI) {
        if (score.wrongVerdict == null) {
            total = score.score.sum()
        }
    }
}

private class SumScoreAccumulator : ScoreAccumulator {
    override var total = 0.0

    override fun add(score: RunResult.IOI) {
        total += score.score.sum()
    }
}

public fun Flow<ContestUpdate>.calculateScoreDifferences(): Flow<ContestUpdate> = withGroupedRuns(
    selector = { it.problemId to it.teamId },
    needUpdateGroup = { new, old, key ->
        new.problems[key.first]?.scoreMergeMode != old?.problems?.get(key.first)?.scoreMergeMode
    },
    transformGroup = transform@{ key, runs, _, contestInfo ->
        if (contestInfo?.resultType != ContestResultType.IOI) return@transform runs
        val accumulator = when (contestInfo.problems[key.first]?.scoreMergeMode ?: ScoreMergeMode.LAST) {
            ScoreMergeMode.MAX_PER_GROUP -> MaxByGroupScoreAccumulator()
            ScoreMergeMode.MAX_TOTAL -> MaxTotalScoreAccumulator()
            ScoreMergeMode.LAST -> LastScoreAccumulator()
            ScoreMergeMode.LAST_OK -> LastOKScoreAccumulator()
            ScoreMergeMode.SUM -> SumScoreAccumulator()
        }

        val results = runs.map {
            if (it.result !is RunResult.IOI) {
                it.result
            } else {
                val before = accumulator.total
                accumulator.add(it.result)
                val after = accumulator.total

                it.result.copy(
                    difference = after - before,
                    scoreAfter = after
                )
            }
        }
        val bestIndex = results.indexOfLast { (it as? RunResult.IOI)?.difference != null && it.difference > 0 }

        runs.zip(results).mapIndexed { index, (run, result) ->
            if (index == bestIndex)
                run.copy(result = (result as RunResult.IOI).copy(isFirstBestTeamRun = true))
            else
                run.copy(result = result)
        }
    }
).map { it.event }