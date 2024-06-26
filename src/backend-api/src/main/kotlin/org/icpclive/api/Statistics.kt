package org.icpclive.api

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
sealed interface SolutionsStatistic {
    val teamsCount: Int
}

@Serializable
data class ICPCProblemSolutionsStatistic(val success: Int, val wrong: Int, val pending: Int)

@Serializable
data class IOIProblemSolutionsStatistic(val result: List<IOIProblemEntity>, val pending: Int)

@Serializable
data class IOIProblemEntity(var count: Int, var score: Double)

@Serializable
@SerialName("ICPC")
data class ICPCSolutionsStatistic(
    override val teamsCount: Int,
    val stats: List<ICPCProblemSolutionsStatistic>
) : SolutionsStatistic

@Serializable
@SerialName("IOI")
data class IOISolutionsStatistic(
    override val teamsCount: Int,
    val stats: List<IOIProblemSolutionsStatistic>
) : SolutionsStatistic
