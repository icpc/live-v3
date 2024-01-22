package org.icpclive.cds.plugins.yandex.api

import kotlinx.serialization.Serializable

@Serializable
internal data class Problems(
    val problems: List<Problem>,
)

@Serializable
internal data class Problem(
    val id: String,
    val alias: String,
    val compilers: List<String>,
    val limits: List<CompilerLimit>,
    val name: String,
    val statements: List<Statement>,
    val testCount: Int?,
)

@Serializable
internal data class CompilerLimit(
    val compilerName: String?,
    val idlenessLimit: Long?,
    val memoryLimit: Long?,
    val outputLimit: Long?,
    val timeLimit: Long?,
)

@Serializable
internal data class Statement(
    val path: String?,
    val type: String?,
)