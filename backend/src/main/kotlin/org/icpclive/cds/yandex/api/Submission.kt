package org.icpclive.cds.yandex.api

import kotlinx.serialization.Serializable

@Serializable
data class Submissions(
    val count: Long,
    val submissions: List<Submission>
)

@Serializable
data class Submission(
    val id: Long,
    val authorId: Long,
    val author: String,
    val compiler: String,
    val problemId: String,
    val problemAlias: String,
    val verdict: String,
    val test: Long,
    val time: Long,
    val memory: Long,
    val timeFromStart: Long
)
