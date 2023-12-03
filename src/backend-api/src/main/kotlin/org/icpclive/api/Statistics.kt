package org.icpclive.api

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
sealed interface ProblemSolutionStatistic

@Serializable
@SerialName("ICPC")
data class ICPCProblemSolutionsStatistic(val success: Int, val wrong: Int, val pending: Int) : ProblemSolutionStatistic

@Serializable
@SerialName("IOI")
data class IOIProblemSolutionsStatistic(val result: List<IOIProblemEntity>, val pending: Int) : ProblemSolutionStatistic

@Serializable
data class IOIProblemEntity(var count: Int, var score: Double)

@Serializable
data class SolutionsStatistic(val stats: List<ProblemSolutionStatistic>)
