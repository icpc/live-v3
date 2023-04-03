package org.icpclive.api

interface ScoreAccumulator {
    fun add(score: IOIRunResult)
    var total : Double
}

class MaxByGroupScoreAccumulator : ScoreAccumulator {
    private val bestByGroup = mutableMapOf<Int, Double>()
    override var total = 0.0

    override fun add(score: IOIRunResult) {
        val byGroup = score.score
        for (g in byGroup.indices) {
            if (bestByGroup.getOrDefault(g, 0.0) < byGroup[g]) {
                total += byGroup[g] - bestByGroup.getOrDefault(g, 0.0)
                bestByGroup[g] = byGroup[g]
            }
        }
    }
}

class MaxTotalScoreAccumulator : ScoreAccumulator {
    override var total = 0.0

    override fun add(score: IOIRunResult) { total = maxOf(total, score.score.sum()) }
}

class LastScoreAccumulator : ScoreAccumulator {
    override var total = 0.0

    override fun add(score: IOIRunResult) { total = score.score.sum() }
}

class LastOKScoreAccumulator : ScoreAccumulator {
    override var total = 0.0

    override fun add(score: IOIRunResult) { if (score.wrongVerdict == null) total = score.score.sum() }
}

class SumScoreAccumulator : ScoreAccumulator {
    override var total = 0.0

    override fun add(score: IOIRunResult) { total += score.score.sum() }
}