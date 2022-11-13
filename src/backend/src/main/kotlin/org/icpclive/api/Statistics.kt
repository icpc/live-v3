package org.icpclive.api

import kotlinx.serialization.Serializable

@Serializable
data class ProblemSolutionsStatistic(val success: Int, val wrong: Int, val pending: Int)

@Serializable
data class SolutionsStatistic(val stats: List<ProblemSolutionsStatistic>)
