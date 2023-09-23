package org.icpclive.api

import kotlinx.serialization.Serializable

@Serializable
sealed interface ProblemSolutionStatistic

@Serializable
data class ICPCProblemSolutionsStatistic(val success: Int, val wrong: Int, val pending: Int) : ProblemSolutionStatistic

@Serializable
data class IOIProblemSolutionsStatistic(val result: List<IOIProblemEntity>, val pending: Int) : ProblemSolutionStatistic

@Serializable
data class IOIProblemEntity(var count: Int, var score: Double)

@Serializable
data class SolutionsStatistic(val stats: List<ProblemSolutionStatistic>)
