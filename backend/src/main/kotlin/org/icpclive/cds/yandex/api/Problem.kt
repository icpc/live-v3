package org.icpclive.cds.yandex.api

import kotlinx.serialization.Serializable
import org.icpclive.api.ProblemInfo

@Serializable
data class Problems(
    val problems: List<Problem>
)

@Serializable
data class Problem(
    val id: String,
    val alias: String,
    val compilers: List<String>,
    val limits: List<CompilerLimit>,
    val name: String,
    val statements: List<Statement>,
    val testCount: Int?
) {
    // TODO: implement color (how? we don't store it)
    fun toProblemInfo() = ProblemInfo(alias, name, null)
}

@Serializable
data class CompilerLimit(
    val compilerName: String?,
    val idlenessLimit: Long?,
    val memoryLimit: Long?,
    val outputLimit: Long?,
    val timeLimit: Long?
)

@Serializable
data class Statement(
    val path: String?,
    val type: String?
)