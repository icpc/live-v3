package org.icpclive.cds.cms.model

import kotlinx.serialization.Serializable

enum class ScoreMode {
    max,
    max_subtask,
}

@Serializable
data class Task(
    val name: String,
    val short_name: String,
    val contest: String,
    val max_score: Double,
    val score_precision: Int,
    val extra_headers: List<String>,
    val order:Int,
    val score_mode: ScoreMode
)