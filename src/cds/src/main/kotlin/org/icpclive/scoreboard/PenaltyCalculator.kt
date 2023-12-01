package org.icpclive.scoreboard

import org.icpclive.api.PenaltyRoundingMode
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes
import kotlin.time.times

internal sealed interface PenaltyCalculator {
    fun addSolvedProblem(time: Duration, wrongAttempts: Int)
    val penalty: Duration

    companion object {
        fun get(penaltyRoundingMode: PenaltyRoundingMode, penaltyPerWrongAttempt: Duration) = when (penaltyRoundingMode) {
            PenaltyRoundingMode.EACH_SUBMISSION_DOWN_TO_MINUTE -> EachSubmissionDownToMinutePenaltyCalculator(penaltyPerWrongAttempt)
            PenaltyRoundingMode.EACH_SUBMISSION_UP_TO_MINUTE -> EachSubmissionUpToMinutePenaltyCalculator(penaltyPerWrongAttempt)
            PenaltyRoundingMode.SUM_DOWN_TO_MINUTE -> SumDownToMinutePenaltyCalculator(penaltyPerWrongAttempt)
            PenaltyRoundingMode.SUM_IN_SECONDS -> SumInSecondsPenaltyCalculator(penaltyPerWrongAttempt)
            PenaltyRoundingMode.LAST -> LastPenaltyCalculator(penaltyPerWrongAttempt)
            PenaltyRoundingMode.ZERO -> ZeroPenaltyCalculator()
        }
    }
}

private class ZeroPenaltyCalculator : PenaltyCalculator {
    override fun addSolvedProblem(time: Duration, wrongAttempts: Int) {}
    override val penalty = Duration.ZERO
}

private class EachSubmissionDownToMinutePenaltyCalculator(private val penaltyPerWrongAttempt: Duration) : PenaltyCalculator {
    override fun addSolvedProblem(time: Duration, wrongAttempts: Int) {
        penalty += time.inWholeMinutes.minutes + penaltyPerWrongAttempt * wrongAttempts
    }

    override var penalty = Duration.ZERO
}

private class EachSubmissionUpToMinutePenaltyCalculator(private val penaltyPerWrongAttempt: Duration) : PenaltyCalculator {
    override fun addSolvedProblem(time: Duration, wrongAttempts: Int) {
        val wholeMinutes = time.inWholeMinutes.minutes
        val extra = if (time != wholeMinutes) 1.minutes else 0.minutes
        penalty += (wholeMinutes + extra) + penaltyPerWrongAttempt * wrongAttempts
    }

    override var penalty = Duration.ZERO
}


private class SumDownToMinutePenaltyCalculator(private val penaltyPerWrongAttempt: Duration) : PenaltyCalculator {
    override fun addSolvedProblem(time: Duration, wrongAttempts: Int) {
        penalty_ += time + wrongAttempts * penaltyPerWrongAttempt
    }

    private var penalty_ = Duration.ZERO
    override val penalty: Duration
        get() = penalty_.inWholeMinutes.minutes
}

private class SumInSecondsPenaltyCalculator(private val penaltyPerWrongAttempt: Duration) : PenaltyCalculator {
    override fun addSolvedProblem(time: Duration, wrongAttempts: Int) {
        penalty += time + wrongAttempts * penaltyPerWrongAttempt
    }

    override var penalty = Duration.ZERO
}

private class LastPenaltyCalculator(private val penaltyPerWrongAttempt: Duration) : PenaltyCalculator {
    private var wrongs = 0
    private var penalty_ = Duration.ZERO
    override fun addSolvedProblem(time: Duration, wrongAttempts: Int) {
        wrongs += wrongAttempts
        penalty_ = maxOf(penalty_, time)
    }

    override val penalty
        get() = penalty_ + wrongs * penaltyPerWrongAttempt
}